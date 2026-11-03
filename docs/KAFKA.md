# Kafka, Transactional Outbox and Idempotent Inbox — EventHub

> **Implementation scope.** Booking Service creates separate Kafka and IBM MQ Outbox records atomically with Booking persistence. Kafka publication and Event Service Inbox consumption were verified locally; IBM MQ has additional broker-backed Testcontainers tests documented in [IBMMQ.md](IBMMQ.md). Kafka DLT persistence and controlled manual republication now have broker-backed integration tests; a complete automated Booking Service → Kafka → Event Service end-to-end and explicit offset-commit verification remain pending.

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
               |-- BookingService.createBooking -> INSERT bookings
               |-- BookingCreatedIntegrationEvent
               |-- ObjectMapper serializes payload to JSON
               |-- OutboxRepository.createOutboxEvent
               |      -> INSERT outbox_events (PENDING)
               +-- ONE PostgreSQL COMMIT / ROLLBACK

OutboxScheduler (@Scheduled, fixedDelay = 5000)
       |-- findByStatusPending
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
       |-- InboxRepository.save(InboxEvent(integrationEventId, processedAt)).block
       |-- INSERT inbox_events (PostgreSQL/R2DBC)
       |      |-- success -> record newly received event
       |      +-- DuplicateKeyException -> ignore redelivery
       +-- other failures -> propagate to Kafka listener error handling
```

## Integration event vs Outbox record

`BookingCreatedIntegrationEvent` is the **integration contract**. It contains four UUIDs: `integrationEventId`, `bookingId`, `eventId` and `userId`. This is intentionally distinct from the internal `Booking` aggregate.

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

`KafkaTemplate.send` is asynchronous. Merely calling `publish` is **not** proof of successful publication. The relay attaches completion callbacks:

```java
integrationEventPublisher.publish(bookingCreatedIntegrationEvent)
.thenRun( -> outboxRepository.markAsPublished(outbox.id))
.exceptionally(throwable -> {
            log.error("Outbox publication failed: {}", outbox.id, throwable);
            return null;
        });
```

The failure callback also handles an exception from `markAsPublished`. The event remains `PENDING` in that case, even if Kafka already received it.

## Guarantees and limitations

**Implemented guarantee:** Booking persistence and Outbox insertion are atomic within the Booking PostgreSQL transaction. A successful local COMMIT leaves an event to publish. The relay retries records still `PENDING` on later polling cycles. Publication is **at least once**, not exactly once.

**Duplicate scenarios:** Kafka may acknowledge an event, but the database update to `PUBLISHED` may fail. Also, if the Kafka future has not completed when the next poll starts, the same `PENDING` event may be sent twice. Multiple scheduler instances increase that possibility. We have **not** implemented a claim/lease (`IN_PROGRESS`) mechanism or coordination among scheduler instances. The Event Service consumer implements persistent deduplication using the unique `integrationEventId` as the Inbox primary key, not the aggregate `bookingId`.

**Other limitations:** the `booking-created.DLT` publication path, bounded listener error-handler tests, DLT persistence and manual replay are implemented. Still missing are a complete automated Booking Service-to-Event Service broker-backed E2E test, independently verified Kafka offset-commit behavior, a protected administrative replay API, multi-instance lease/fencing hardening for stale `PROCESSING` records, guaranteed publication deadlines and full poison-message handling. A malformed stored payload can currently interrupt that polling invocation. Kafka availability is not guaranteed; acknowledgments and retries do not guarantee eventual success under permanent failure.

## Evidence 

- `JpaOutboxRepositoryAdapterIT`: real PostgreSQL/Testcontainers persistence and `markAsPublished` verification (green).
- `TransactionalBookingUseCaseIT`: verified Booking and pending Outbox row persisted together; simulated `OutboxRepository.createOutboxEvent` failure rolled back Booking and triggered remote-release request through mocked capacity port (green).
- Manual local scheduler check: repeatedly found `PENDING` event, deserialized its JSON and verified the `bookingId` equals `aggregateId`.
- Manual local Kafka publication check: log showed the Outbox found, then published; database row verified as `PUBLISHED`.

These are scoped tests and manual observations. They do not constitute an automated end-to-end Kafka integration test or proof against all crash windows.

## Event Service: Kafka consumer and Inbox

The independent **Event Service** uses Spring WebFlux and R2DBC for its Event API and persistence. Kafka consumption is handled by a **Spring Kafka imperative `@KafkaListener`**, rather than by a reactive Kafka receiver. The listener is a Kafka-managed thread: `InboxRepository.save(...).block` waits for the R2DBC INSERT to finish before returning to Kafka's listener container. Do not call `subscribe` and immediately return, because the offset could advance before the Inbox write is finished.

- **Topic:** `booking-created`. **Consumer group:** `eventhub-event-service`.
- **Deserializers:** Kafka `StringDeserializer` for key and value; Jackson `ObjectMapper` converts the JSON payload to `BookingCreatedIntegrationEvent(integrationEventId, bookingId, eventId, userId)` in Event Service.
- **Deduplication ID:** `integrationEventId`, generated once per logical integration event and retained in the serialized Outbox payload across retries. `bookingId` identifies the Booking aggregate; `eventId` identifies the business Event being booked. Multiple integration events may legitimately share a `bookingId`.
- **Application model and port:** `InboxEvent(UUID integrationEventId, OffsetDateTime processedAt)` and `InboxRepository` (`Mono<Boolean> existsById(UUID)` and `Mono<InboxEvent> save(InboxEvent)`). Here `InboxEvent.integrationEventId` is the **message deduplication identifier**, populated from the integration event payload.
- **R2DBC infrastructure:** `InboxEntity` maps `inbox_events`, implements `Persistable<UUID>` and supplies `isNew=true` for new INSERTs; `SpringDataInboxRepository` and `R2dbcInboxRepositoryAdapter` implement the port.
- **Schema:** `inbox_events(integration_event_id UUID PRIMARY KEY, processed_at TIMESTAMPTZ NOT NULL)` in Event Service's `schema.sql`. The primary key enforces uniqueness, including when duplicate deliveries race.
- **Listener:** `BookingCreatedKafkaConsumer` is an `@Component` with `@KafkaListener(topics="booking-created", groupId="eventhub-event-service")`. It deserializes the JSON, creates an `InboxEvent` using `integrationEventId`, and calls `inboxRepository.save(inboxEvent).block`. `DuplicateKeyException` is treated as an already-recorded message; unrelated exceptions are not swallowed.

```text
booking-created JSON
        ↓
BookingCreatedKafkaConsumer @KafkaListener
        ↓ deserialize
BookingCreatedIntegrationEvent
        ↓ integrationEventId -> InboxEvent.integrationEventId
InboxRepository.save(...).block
        ├── INSERT succeeds  -> log new event
        ├── duplicate key    -> log ignored duplicate
        └── other failure    -> throw (listener error handling applies)
```

The earlier `existsById -> save` implementation was simplified: a pre-check is vulnerable to a check-then-insert race and adds a database query. The PostgreSQL primary key plus a narrowly scoped `DuplicateKeyException` handler is the deduplication mechanism actually retained. The `existsById` port method remains available, but is not required in the final listener path.

**Scope of this exercise:** receiving and recording events only. Event capacity was already reserved over REST; the consumer must **not** reserve it again. No downstream business side effect is executed. If one is introduced later, it must be committed atomically with the Inbox record using an appropriate reactive transaction.

**Note on delivery semantics:** the Inbox prevents two persisted records for the same key, not repeated deliveries from Kafka. The Kafka listener error-handler configuration has been exercised by mocked unit tests; actual Kafka offset behavior has not been verified with a broker-backed failure-injection test. `auto-offset-reset: earliest` applies only when a group has no committed offset.

## Consumer-side evidence 

- `R2dbcInboxRepositoryAdapterIT`: PostgreSQL/Testcontainers tests passed for saving an event, checking existence, and rejecting a duplicate primary key; the missing `testcontainers-r2dbc` dependency was added to enable the R2DBC service connection.
- Manual two-service verification: Event Service received the producer payload from the `booking-created` topic; the payload carries both the aggregate `bookingId` and the independent `integrationEventId`.
- Manual Inbox verification: an incoming Booking-created event was persisted in `inbox_events`. Repeating the same logical event was logged as ignored; the database row was not duplicated.
- `BookingCreatedKafkaConsumerTest`: mocked repository verifies two delivery attempts failing and a third succeeding, plus two events sharing `bookingId` but carrying distinct `integrationEventId` values. These calls are invoked explicitly by the test; they do not prove broker-driven retry behavior.
- `KafkaErrorHandlerConfigurationTest`: tests non-retryable malformed JSON, two retries, DLT publishing after exhaustion and failure when DLT publication fails. These are error-handler unit tests, not broker-backed end-to-end evidence.
- **Not verified:** full automated two-service Kafka E2E tests, Kafka offset-commit behavior under failure, concurrent duplicate handling under load, or exactly-once semantics. **Now verified separately:** broker-backed DLT persistence/retry and controlled republication, as detailed below.

## Next learning milestone

DLT persistence and manual controlled replay are now implemented and integration-tested. Next: multi-instance replay hardening, poison-message handling, technical DLT header filtering, explicit offset-commit verification, and full two-service E2E tests. Preserve the logical `integrationEventId` on replay.


## Integration-event identity

`TransactionalBookingUseCase` generates `UUID.randomUUID` once for each new `BookingCreatedIntegrationEvent`. Its four-field JSON payload is stored in both broker-specific Outbox rows. `aggregateId` remains the Booking UUID, while the new `integrationEventId` identifies this logical integration event across deliveries. Booking Service has a Flyway V6 Inbox-column rename; Event Service uses `integration_event_id` in `schema.sql` and its R2DBC `InboxEntity`. Database migrations for an existing deployment must preserve its historic Inbox data.

The Kafka listener inserts the Inbox row directly and ignores only `DuplicateKeyException`; other persistence errors propagate to the configured Kafka error handler. The consumer does not yet perform a downstream business effect. If a business effect is added, Inbox insertion and that effect require coordinated atomicity.


## DLT persistence and manual recovery
**Scope:** Event Service; these tests exercise Kafka/PostgreSQL Testcontainers (and Toxiproxy for database-failure injection), not a complete two-service end-to-end workflow.

- `DltRecoveryConsumer` subscribes to `booking-created.DLT` in group `eventhub-dlt-recovery`. It persists DLT records in PostgreSQL `recovery_events` keyed by source topic, partition and offset. It retains message key, raw payload and Kafka headers encoded by `KafkaHeadersCodec`. Duplicate persistence is ignored via `DuplicateKeyException`.
- A dedicated `dltRecoveryKafkaListenerContainerFactory` uses `DefaultErrorHandler` with an unlimited `FixedBackOff` for transient database persistence failures. In `DltRecoveryRetryIT`, Toxiproxy interrupted PostgreSQL access: database calls failed, repeated delivery was observed, and the original DLT event was persisted after recovery.
- `EventRecoveryEntity` models `PENDING → PROCESSING → REPUBLISHED`, with `attemptId`, timestamps and `@Version` optimistic locking. `DltRecoveryService.reprocess` finds `PENDING` entries sequentially with Reactor `concatMap`, claims each with a persisted `PROCESSING` transition, sends a `ProducerRecord<String,String>` to `booking-created`, and marks it `REPUBLISHED` only after Kafka acknowledges. The record reconstructs original key, payload and headers via `KafkaHeadersCodec.deserialize`.
- On publication failure the service attempts to restore `PENDING`; if saving `REPUBLISHED` fails **after Kafka ACK**, that failure propagates and the database can retain `PROCESSING`. This is an ambiguous delivery window, not exactly-once delivery.
- `DltRecoveryServiceIT` (five passing integration tests): successful republish and independent consumer verification of key, payload, and `correlationId` header bytes; simulated Kafka publish failure restores `PENDING`; stale `@Version` claim fails without publishing; simulated PostgreSQL failure after Kafka ACK leaves persisted `PROCESSING` and propagates an error. The failure scenarios use targeted Mockito spies with real PostgreSQL; the happy-path publication and consumer use real Kafka.

### Scheduled unlocking of stale recovery claims (verified 2026-11-03)

`DltRecoveryService.unblockExpired()` queries recovery records with status `PROCESSING` and calculates a cutoff at `Instant.now().minusSeconds(300L)`. For each record with `processingAt.isBefore(cutoff)`, it transitions the entity back to `PENDING` through `markAsPendingFromProcessing()`, which clears `attemptId` and `processingAt` and refreshes `pendingAt`, and saves it with Spring Data R2DBC `@Version`. The `concatMap` flow is sequential. An `OptimisticLockingFailureException` during an individual save is logged and ignored; other failures propagate. A `PROCESSING` record with null `processingAt` is logged as inconsistent and skipped rather than silently repaired.

`DltRecoveryScheduler.processUnblockExpired()` invokes this service on a Spring `@Scheduled` cadence. Its `AtomicBoolean.compareAndSet(false, true)` guards against overlapping invocations **within one application instance**. Because it subscribes to an asynchronous `Mono`, it uses `doFinally(signalType -> running.set(false))` to release the guard on completion, error or cancellation. Errors are logged by the subscriber.

**Important scope boundary:** the scheduler performs **unlock only**; it does not automatically invoke `reprocess()` or guarantee eventual publication. A subsequent invocation of `reprocess()` must publish the restored `PENDING` events. A record that was acknowledged by Kafka before its final database update failed can be sent again after timeout; existing consumer deduplication by stable `integrationEventId` helps but does not make broker publication exactly-once.

**Verification:** `DltRecoveryUnblockIT` covers stale unlock, a fresh `PROCESSING` entry left untouched, optimistic version conflicts, and an inconsistent null `processingAt` left unchanged. `DltRecoverySchedulerTest` exercises overlap suppression and guard release after a reactive error. The full `mvn verify` execution completed GREEN on 2026-11-03. This does not establish cross-instance coordination or protection against every crash window.

**Still pending:** simultaneous replay test across two executions/instances; multi-instance lease and fencing for stale `PROCESSING`; end-to-end deduplication of ambiguous ACK outcomes; distinguishing malformed headers/payload from transient failures and quarantining unrecoverable records; filtering DLT error metadata while retaining business headers; administrative replay triggering/authorization and metrics (periodic unlock scheduling is already implemented). Original Booking Service Outbox relay coordination is a separate pending concern.
