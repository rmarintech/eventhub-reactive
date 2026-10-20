# Kafka, Transactional Outbox and Idempotent Inbox — EventHub

> **Implementation scope (updated 20 October 2026).** Booking Service creates separate Kafka and IBM MQ Outbox records atomically with Booking persistence. Kafka publication and Event Service Inbox consumption were verified locally; IBM MQ has additional broker-backed Testcontainers tests documented in [IBMMQ.md](IBMMQ.md). The Kafka path still does **not** have a complete automated broker-backed end-to-end test.

## Dual-broker Outbox evolution

Booking creation now inserts **two** Outbox rows in the same local JPA transaction: `broker_name=KAFKA` and `broker_name=IBMMQ` (Flyway V5), using the same `BookingCreatedIntegrationEvent` payload but independent Outbox IDs. `OutboxKafkaScheduler` publishes to Kafka through `KafkaIntegrationEventAdapter`; `OutboxIBMMQScheduler` publishes to IBM MQ through `IbmMqIntegrationEventAdapter`. Each record is acknowledged/marked independently. This replaces the earlier single `OutboxScheduler` and direct-only IBM MQ publishing design. The historical single-broker examples below explain Kafka mechanics; **the current runtime uses broker-specific schedulers**.

Two local Outbox inserts are atomic with Booking creation. Two broker sends are **not** atomic with each other or with the database transaction. Failures may yield differing broker publication states; duplicate publication is possible. `KAFKA` consumer Inbox and Booking Service's `IBMMQ` consumer Inbox are separate stores. See [IBMMQ.md](IBMMQ.md).

## Why we use Kafka

Booking Service creates a Booking synchronously after reserving capacity through REST in Event Service. Kafka is a separate **asynchronous integration channel** for notifying other services that a Booking was created. Kafka does **not** replace the REST capacity reservation or compensation operation.

```text
Client -> BookingController
               |
               v
      CompensatingBookingUseCase
               | reserve capacity via REST -> Event Service
               v
      TransactionalBookingUseCase (@Transactional)
               |-- BookingService.createBooking() -> INSERT bookings
               |-- BookingCreatedIntegrationEvent
               |-- ObjectMapper serializes payload to JSON
               |-- OutboxRepository.createOutboxEvent()
               |      -> INSERT outbox_events (PENDING)
               +-- ONE PostgreSQL COMMIT / ROLLBACK

OutboxScheduler (@Scheduled, fixedDelay = 5000)
       |-- findByStatusPending()
       |-- ObjectMapper.readValue(payload, BookingCreatedIntegrationEvent.class)
       |-- IntegrationEventPublisherPort.publish(event)
       v
KafkaIntegrationEventAdapter
       |-- KafkaTemplate.send("booking-created", bookingId, JSON)
       +-- CompletableFuture<Void> (acknowledgment or exceptional completion)
                     | success -> markAsPublished(outboxId)
                     | failure -> log error; record stays PENDING
       v
Kafka topic: booking-created
       v
Event Service @KafkaListener (group: eventhub-event-service)
       |-- ObjectMapper -> BookingCreatedIntegrationEvent
       |-- InboxRepository.save(InboxEvent(bookingId, processedAt)).block()
       |-- INSERT inbox_events (PostgreSQL/R2DBC)
       |      |-- success -> record newly received event
       |      +-- DuplicateKeyException -> ignore redelivery
       +-- other failures -> propagate to Kafka listener error handling
```

## Integration event vs Outbox record

`BookingCreatedIntegrationEvent` is the **integration contract**. It contains three UUIDs: `bookingId`, `eventId` and `userId`. This is intentionally distinct from the internal `Booking` aggregate.

`OutboxEvent` is a **publication record** with `OutboxEventId`, `eventType` (`BookingCreated`), `aggregateId` (the Booking UUID), `topic` (`booking-created`), `payload` (JSON), `status`, `createdAt` and `publishedAt`. Current statuses are `PENDING` and `PUBLISHED`.

We first create the integration event to obtain its serialized JSON. We insert its Outbox record **in the same local JPA transaction** as the Booking. The Outbox table is created by Flyway V3; a `(status, created_at)` index supports polling.

## Application ports and infrastructure adapters

- **Application:** `IntegrationEvent` marker, `BookingCreatedIntegrationEvent` record, `IntegrationEventPublisherPort` and `OutboxRepository` abstractions; `OutboxEvent`, `OutboxEventId` and status model.
- **Infrastructure — PostgreSQL:** `OutboxJpaEntity`, `SpringDataOutboxRepository`, `JpaOutboxRepositoryAdapter`; `createOutboxEvent`, `findByStatusPending`, `markAsPublished`.
- **Infrastructure — Kafka:** `KafkaIntegrationEventAdapter` implements `IntegrationEventPublisherPort`, using `KafkaTemplate<String, String>` and `ObjectMapper`.
- **Infrastructure — polling:** `OutboxScheduler` uses `@Component`, `@Scheduled(fixedDelay = 5000)` and application ports; `@EnableScheduling` is required to activate scheduled methods.

The application port returns `CompletableFuture<Void>` rather than Kafka's `SendResult<String, String>`. This preserves the architecture boundary: `CompletableFuture` is standard Java, whereas `SendResult` belongs to Spring Kafka.

```java
// Application port
CompletableFuture<Void> publish(IntegrationEvent event);

// Kafka adapter: only complete successfully when Kafka acknowledges the send
return kafkaTemplate.send(topic, key, payload)
        .thenApply(sendResult -> null);
```

`KafkaTemplate.send()` is asynchronous. Merely calling `publish()` is **not** proof of successful publication. The relay attaches completion callbacks:

```java
integrationEventPublisher.publish(bookingCreatedIntegrationEvent)
        .thenRun(() -> outboxRepository.markAsPublished(outbox.id()))
        .exceptionally(throwable -> {
            log.error("Outbox publication failed: {}", outbox.id(), throwable);
            return null;
        });
```

The failure callback also handles an exception from `markAsPublished()`. The event remains `PENDING` in that case, even if Kafka already received it.

## Guarantees and limitations

**Implemented guarantee:** Booking persistence and Outbox insertion are atomic within the Booking PostgreSQL transaction. A successful local COMMIT leaves an event to publish. The relay retries records still `PENDING` on later polling cycles. Publication is **at least once**, not exactly once.

**Duplicate scenarios:** Kafka may acknowledge an event, but the database update to `PUBLISHED` may fail. Also, if the Kafka future has not completed when the next poll starts, the same `PENDING` event may be sent twice. Multiple scheduler instances increase that possibility. We have **not** implemented a claim/lease (`IN_PROGRESS`) mechanism or coordination among scheduler instances. The Event Service consumer implements persistent deduplication using the Booking UUID as the Inbox primary key.

**Other limitations:** no dead-letter topic, automated Kafka integration-test suite, verified consumer error-retry/offset-commit policy, guaranteed publish deadline or robust poison-message policy has been implemented yet. A malformed stored payload can currently interrupt that polling invocation. Kafka availability is not guaranteed; acknowledgments and retries do not guarantee eventual success under permanent failure.

## Evidence 

- `JpaOutboxRepositoryAdapterIT`: real PostgreSQL/Testcontainers persistence and `markAsPublished` verification (green).
- `TransactionalBookingUseCaseIT`: verified Booking and pending Outbox row persisted together; simulated `OutboxRepository.createOutboxEvent()` failure rolled back Booking and triggered remote-release request through mocked capacity port (green).
- Manual local scheduler check: repeatedly found `PENDING` event, deserialized its JSON and verified the `bookingId` equals `aggregateId`.
- Manual local Kafka publication check: log showed the Outbox found, then published; database row verified as `PUBLISHED`.

These are scoped tests and manual observations. They do not constitute an automated end-to-end Kafka integration test or proof against all crash windows.

## Event Service: Kafka consumer and Inbox

The independent **Event Service** uses Spring WebFlux and R2DBC for its Event API and persistence. Kafka consumption is handled by a **Spring Kafka imperative `@KafkaListener`**, rather than by a reactive Kafka receiver. The listener is a Kafka-managed thread: `InboxRepository.save(...).block()` waits for the R2DBC INSERT to finish before returning to Kafka's listener container. Do not call `subscribe()` and immediately return, because the offset could advance before the Inbox write is finished.

- **Topic:** `booking-created`. **Consumer group:** `eventhub-event-service`.
- **Deserializers:** Kafka `StringDeserializer` for key and value; Jackson `ObjectMapper` converts the JSON payload to `BookingCreatedIntegrationEvent(bookingId, eventId, userId)` in Event Service.
- **Deduplication ID:** `bookingId` because this exercise generates one `BookingCreatedIntegrationEvent` per Booking. Do not confuse it with the business `eventId` (the Event whose capacity was previously reserved).
- **Application model and port:** `InboxEvent(UUID eventId, OffsetDateTime processedAt)` and `InboxRepository` (`Mono<Boolean> existsById(UUID)` and `Mono<InboxEvent> save(InboxEvent)`). Here `InboxEvent.eventId` is the **message deduplication identifier**, populated from `bookingId`.
- **R2DBC infrastructure:** `InboxEntity` maps `inbox_events`, implements `Persistable<UUID>` and supplies `isNew=true` for new INSERTs; `SpringDataInboxRepository` and `R2dbcInboxRepositoryAdapter` implement the port.
- **Schema:** `inbox_events(event_id UUID PRIMARY KEY, processed_at TIMESTAMPTZ NOT NULL)` in Event Service's `schema.sql`. The primary key enforces uniqueness, including when duplicate deliveries race.
- **Listener:** `BookingCreatedKafkaConsumer` is an `@Component` with `@KafkaListener(topics="booking-created", groupId="eventhub-event-service")`. It deserializes the JSON, creates an `InboxEvent` using `bookingId`, and calls `inboxRepository.save(inboxEvent).block()`. `DuplicateKeyException` is treated as an already-recorded message; unrelated exceptions are not swallowed.

```text
booking-created JSON
        ↓
BookingCreatedKafkaConsumer @KafkaListener
        ↓ deserialize
BookingCreatedIntegrationEvent
        ↓ bookingId -> InboxEvent.eventId
InboxRepository.save(...).block()
        ├── INSERT succeeds  -> log new event
        ├── duplicate key    -> log ignored duplicate
        └── other failure    -> throw (listener error handling applies)
```

The earlier `existsById() -> save()` implementation was simplified: a pre-check is vulnerable to a check-then-insert race and adds a database query. The PostgreSQL primary key plus a narrowly scoped `DuplicateKeyException` handler is the deduplication mechanism actually retained. The `existsById()` port method remains available, but is not required in the final listener path.

**Scope of this exercise:** receiving and recording events only. Event capacity was already reserved over REST; the consumer must **not** reserve it again. No downstream business side effect is executed. If one is introduced later, it must be committed atomically with the Inbox record using an appropriate reactive transaction.

**Note on delivery semantics:** the Inbox prevents two persisted records for the same key, not repeated deliveries from Kafka. The actual Kafka offset/error behavior depends on listener-container configuration and has not been failure-injection-tested. `auto-offset-reset: earliest` applies only when a group has no committed offset.

## Consumer-side evidence 

- `R2dbcInboxRepositoryAdapterIT`: PostgreSQL/Testcontainers tests passed for saving an event, checking existence, and rejecting a duplicate primary key; the missing `testcontainers-r2dbc` dependency was added to enable the R2DBC service connection.
- Manual two-service verification: Event Service logged the same `bookingId` as the producer payload from the `booking-created` topic.
- Manual Inbox verification: an incoming Booking-created event was persisted in `inbox_events`. Repeating the same logical event was logged as ignored; the database row was not duplicated.
- **Not verified:** automated broker-backed consumer integration tests, Kafka offset-commit behavior under failure, concurrent duplicate handling under load, a DLT, or exactly-once semantics.

## Next learning milestone

Introduce Dead Letter Topic (DLT) handling and focused tests for poison messages and transient consumer failures. Preserve the current producer and Inbox architecture.

