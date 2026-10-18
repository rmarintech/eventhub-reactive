# Kafka and Transactional Outbox — EventHub

> **Current implementation — 18 October 2026.** This document records what was implemented and manually verified in the independent `booking-service`. It distinguishes producer publication from consumer delivery, and asynchronous acknowledgments from local database transactions.

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
Future consumer (NOT implemented yet)
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

**Duplicate scenarios:** Kafka may acknowledge an event, but the database update to `PUBLISHED` may fail. Also, if the Kafka future has not completed when the next poll starts, the same `PENDING` event may be sent twice. Multiple scheduler instances increase that possibility. We have **not** implemented a claim/lease (`IN_PROGRESS`) mechanism or coordination among scheduler instances. Future consumers must therefore be idempotent.

**Other limitations:** no consumer, consumer group, consumer-side idempotency implementation, dead-letter topic, Kafka integration-test suite, guaranteed publish deadline or robust poison-message policy has been implemented yet. A malformed stored payload can currently interrupt that polling invocation. Kafka availability is not guaranteed; acknowledgments and retries do not guarantee eventual success under permanent failure.

## Evidence as of 18 October 2026

- `JpaOutboxRepositoryAdapterIT`: real PostgreSQL/Testcontainers persistence and `markAsPublished` verification (green).
- `TransactionalBookingUseCaseIT`: verified Booking and pending Outbox row persisted together; simulated `OutboxRepository.createOutboxEvent()` failure rolled back Booking and triggered remote-release request through mocked capacity port (green).
- Manual local scheduler check: repeatedly found `PENDING` event, deserialized its JSON and verified the `bookingId` equals `aggregateId`.
- Manual local Kafka publication check: log showed the Outbox found, then published; database row verified as `PUBLISHED`.

These are scoped tests and manual observations. They do not constitute an automated end-to-end Kafka integration test or proof against all crash windows.

## Next learning milestone

Implement a Kafka consumer for `booking-created` and test consumer-side idempotency. Keep the existing producer flow unchanged while studying this separately.
