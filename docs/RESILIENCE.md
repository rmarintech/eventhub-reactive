# Inter-service Resilience — EventHub


**19 October 2026:** Kafka consumer-side deduplication is implemented in Event Service using a PostgreSQL/R2DBC Inbox. This does not alter the REST GET-availability resilience decorators. Kafka error-handler / DLT behavior remains to be implemented and tested; see [KAFKA.md](KAFKA.md).

**Kafka boundary:** the separate Kafka Outbox producer does not use the GET-availability Bulkhead/Circuit Breaker/Retry decorator chain documented below. REST capacity reserve/release remains synchronous. Kafka and Outbox publishing reliability are documented in [KAFKA.md](KAFKA.md).

This document explains the **synchronous Booking Service → Event Service** resilience work implemented and tested on 15–16 October 2026, including Bulkhead. It collects theory, concrete configuration, tested behavior and limitations in one place. It does **not** describe Reactor's reactive `retry()` operator; that belongs in [`REACTIVE.md`](REACTIVE.md).

## Scope and architecture

- Caller: independent **Booking Service** (Spring MVC, blocking `RestClient`, port 8081).
- Callee: **Event Service** (WebFlux/R2DBC, port 8080), queried through `GET /events/{id}`.
- Application contract: `EventAvailabilityPort.isAvailable(EventId)` returns `boolean`.
- `true` means Event has `PUBLISHED` status and `available > 0`; `false` is a *valid technical response* for a non-bookable Event.
- REST availability checks **do not reserve capacity**. A separate Event-capacity POST path has since been implemented for booking creation; see the current-flow section below. The GET-specific resilience decorators are **not** applied to mutating POST calls.

```text
BookingService (application)
  → EventAvailabilityPort
    → BulkheadEventAvailabilityAdapter        [outermost; 5 permits]
      → CircuitBreakerEventAvailabilityAdapter
        → RetryableEventAvailabilityAdapter   [up to 3 attempts]
          → RestEventAvailabilityAdapter
            → RestClient → GET /events/{id} → Event Service
```

All four adapters implement the same output port. `EventClientConfiguration` wires them using Resilience4j's **core Java API**, without Spring AOP/@Retry/@CircuitBreaker annotations. The application layer remains independent of HTTP and Resilience4j. See [`HEXAGONAL.md`](HEXAGONAL.md#booking-service-circuit-breaker-and-resilience-decorators-16-october-2026) for the dependency-inversion decision.

## What resilience means here

**Transient failures** may recover after a short wait (e.g., temporary network/remote server failures); a bounded Retry can help. **Persistent or frequent failures** make repeated calls counterproductive; Circuit Breaker can stop new attempts temporarily. **Timeouts** cap the wait for an individual network operation. These mechanisms improve failure handling but are not substitutes for distributed transactions or atomic capacity reservations. Bulkhead adds bounded resource isolation for the caller.

## HTTP timeouts — RestClient

`EventClientConfiguration` configures `SimpleClientHttpRequestFactory`:

```java
factory.setConnectTimeout(Duration.ofSeconds(2));
factory.setReadTimeout(Duration.ofSeconds(3));
```

Connect timeout limits establishing a connection; read timeout limits blocking reads according to the chosen request factory. These are **per-attempt** settings, *not* a verified overall operation deadline. Real timeout expiration has not been timing-tested.

The HTTP adapter requests a minimal `EventResponse(UUID id, String status, int available)` and maps outcomes:

| Outcome | Application result | HTTP response from Booking |
| --- | --- | --- |
| PUBLISHED, places available | `true` | Booking may proceed |
| DRAFT, CANCELLED, or no places | `false` | 409 `EVENT_NOT_AVAILABLE` |
| Remote Event not found (404) | `EventNotFoundException` | 404 `EVENT_NOT_FOUND` |
| Remote HTTP 5xx / I/O failure | `EventServiceUnavailableException` | 503 `EVENT_SERVICE_UNAVAILABLE` |
| Circuit Breaker OPEN | `EventServiceUnavailableException` (cause: `CallNotPermittedException`) | 503 `EVENT_SERVICE_UNAVAILABLE` |
| Bulkhead full | `EventServiceUnavailableException` (cause: `BulkheadFullException`) | 503 `EVENT_SERVICE_UNAVAILABLE` |

`RestEventAvailabilityAdapterTest` uses `MockRestServiceServer` for Event status/availability variants, extra JSON fields, HTTP 404/500 and simulated I/O failure. Other 4xx, especially 429, still need a deliberate mapping policy.

## Retry — Resilience4j

**Purpose:** handle a failure that may be temporary, without infinite repeated calls.

Implemented with `resilience4j-retry` **2.3.0**, `RetryConfig` and `Retry.decorateSupplier(...)` in `RetryableEventAvailabilityAdapter`, surrounding the REST port delegate.

```java
RetryConfig config = RetryConfig.custom()
        .maxAttempts(3)
        .waitDuration(Duration.ofMillis(500))
        .retryExceptions(EventServiceUnavailableException.class)
        .build();
```

- **3 total attempts** means one initial execution and at most **2 retries**, not three additional retries.
- Wait **500 ms** between attempts, currently fixed rather than exponential/jittered.
- Retry `EventServiceUnavailableException` only (remote 5xx and transport failures as currently mapped).
- Do not retry successful `true`, valid `false`, or remote 404 represented by `EventNotFoundException`.
- Once attempts are exhausted, propagate `EventServiceUnavailableException` to the existing 503 handler.

The adapter uses the **Decorator pattern**:

```java
@Override
public boolean isAvailable(EventId eventId) {
    return Retry.decorateSupplier(retry, () -> delegate.isAvailable(eventId)).get();
}
```

**Verified:** `RetryableEventAvailabilityAdapterTest` tests immediate `true`, immediate `false`, two transient failures then recovery, three failures, and remote 404 without retry. It uses zero delay in unit-test configuration to keep tests fast. Retries are synchronous and may increase request latency.

## Circuit Breaker — Resilience4j

**Purpose:** stop sending new requests temporarily when recent calls indicate that the dependency is unhealthy. Unlike Retry, it does not repeat a failed call.

Implemented with `resilience4j-circuitbreaker` **2.3.0**, `CircuitBreaker.decorateSupplier(...)` and `CircuitBreakerEventAvailabilityAdapter` around Retry, inside the outer Bulkhead.

### States

```text
CLOSED    calls are permitted; record relevant outcomes
   │ failure rate reaches threshold after sufficient calls
   ▼
OPEN      reject immediately; do not call Retry/HTTP
   │ waiting interval passes; on the next permitted state check/call
   ▼
HALF_OPEN allow a limited number of probe calls
   ├─ healthy enough → CLOSED
   └─ threshold met → OPEN
```

A single failure does not automatically open the production circuit: the configured minimum call count matters. `false` is still a technically successful call and does not count as a server failure.

### Production configuration

```java
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .slidingWindowSize(10)
        .minimumNumberOfCalls(10)
        .waitDurationInOpenState(Duration.ofSeconds(10))
        .permittedNumberOfCallsInHalfOpenState(2)
        .recordExceptions(EventServiceUnavailableException.class)
        .ignoreExceptions(EventNotFoundException.class)
        .build();
```

| Setting | Meaning |
| --- | --- |
| Failure threshold 50% | Open when recorded technical failures reach 50% of evaluated calls |
| Count-based window 10 | Evaluate a rolling window of the most recent 10 recorded calls |
| Minimum calls 10 | Do not apply failure-rate threshold before 10 recorded calls |
| OPEN wait 10 s | Wait before allowing a HALF_OPEN transition |
| HALF_OPEN permitted calls 2 | Allow 2 probes to assess recovery |
| Record `EventServiceUnavailableException` | Count mapped HTTP 5xx/transport failures as technical errors |
| Ignore `EventNotFoundException` | Remote 404 is neither a technical success nor a technical failure in breaker statistics |

Exceptions outside the explicit recorded/ignored categories follow the configured Resilience4j defaults; this is not a blanket assertion that all exceptions other than the listed ones are ignored.

### Why Circuit Breaker wraps Retry

The breaker (outside Retry, inside Bulkhead) receives **one final result from the logical operation**:

```text
Circuit Breaker
 └─ Retry
     ├─ HTTP attempt 1: 503
     ├─ HTTP attempt 2: 503
     └─ HTTP attempt 3: success
 Circuit Breaker records: one successful logical call
```

If all three attempts fail, the breaker records **one** failure; if OPEN, no HTTP attempts take place. Reversing the order would alter failure accounting and the behavior of retries around OPEN rejections.

The Circuit Breaker adapter translates OPEN rejection without counting that rejection as a fresh backend failure:

```java
@Override
public boolean isAvailable(EventId eventId) {
    try {
        return CircuitBreaker.decorateSupplier(
                circuitBreaker, () -> delegate.isAvailable(eventId)).get();
    } catch (CallNotPermittedException ex) {
        throw new EventServiceUnavailableException(ex);
    }
}
```

### Tests completed

`CircuitBreakerEventAvailabilityAdapterTest` uses JUnit 5 + Mockito **without Spring**, with a simplified `slidingWindowSize(2)`/`minimumNumberOfCalls(2)` to exercise state transitions quickly:

| Scenario | Assertions |
| --- | --- |
| Two failures | `CLOSED → OPEN`; next call translated from `CallNotPermittedException`; delegate called twice |
| Successful recovery | Force `HALF_OPEN`; two successes → `CLOSED` |
| Unsuccessful recovery | Force `HALF_OPEN`; one failure + one success → `OPEN` at 50%; next call rejected |
| Ignored 404 | Repeated `EventNotFoundException` leaves `CLOSED` and does not count as a failure or success when ignored |

`EventClientConfigurationTest` uses `@SpringBootTest` to check the relevant Spring beans and verify that the outer `EventAvailabilityPort` bean is the Bulkhead decorator. It does not independently inspect every nested adapter. For HALF_OPEN tests the transition is **forced manually**; the actual 10-second automatic transition is not verified.

## Bulkhead — Resilience4j (SemaphoreBulkhead)

**Purpose:** bound the number of concurrent logical operations that Booking Service sends through its Event-availability dependency. Unlike Circuit Breaker, Bulkhead does not determine whether Event Service is healthy; it prevents too many ongoing requests from consuming Booking resources at the same time. It limits concurrent **operations**, not the total number of threads in the application.

The imperative Booking service uses `resilience4j-bulkhead` **2.3.0** and `Bulkhead.decorateSupplier(...)` in `BulkheadEventAvailabilityAdapter`. The production configuration is:

```java
BulkheadConfig config = BulkheadConfig.custom()
        .maxConcurrentCalls(5)
        .maxWaitDuration(Duration.ZERO)
        .build();

return Bulkhead.of("eventAvailability", config);
```

- Up to **5 logical Event-availability operations at once**, per Booking Service instance. The sixth concurrent operation is rejected immediately when all permits are held; there is no waiting queue at this semaphore.
- `BulkheadFullException` is translated to `EventServiceUnavailableException(ex)`, preserving the original rejection as the cause. Existing HTTP exception handling returns `503 EVENT_SERVICE_UNAVAILABLE`.
- The decorator is the **outermost** layer: `Bulkhead → Circuit Breaker → Retry → REST`. A permit is held throughout the complete Retry operation, including waits between attempts. A Bulkhead rejection never reaches the inner breaker or HTTP adapter.
- The configuration is **not a cluster-wide limit**. Multiple Booking replicas have independent permits. Event Service needs its own protection and capacity management when relevant.

### Bulkhead adapter

```java
@Override
public boolean isAvailable(EventId eventId) {
    try {
        return Bulkhead.decorateSupplier(
                bulkhead, () -> delegate.isAvailable(eventId)).get();
    } catch (BulkheadFullException ex) {
        throw new EventServiceUnavailableException(ex);
    }
}
```

### Tests completed

`BulkheadEventAvailabilityAdapterTest` uses Mockito, `ExecutorService`, `Future` and two `CountDownLatch` objects without Spring. Its test-specific limit is **1 permit**: one call enters the mocked delegate and waits; a concurrent second call is rejected with an `EventServiceUnavailableException` whose cause is `BulkheadFullException`; releasing the first call restores the permit; Mockito verifies that only the admitted call reached the delegate. Latches avoid reliance on `Thread.sleep()`. The test was reported green.

`EventClientConfigurationTest` (`@SpringBootTest`) verifies that the Retry, Circuit Breaker, Bulkhead and RestClient beans load and that the `EventAvailabilityPort` bean exposed by Spring is a `BulkheadEventAvailabilityAdapter`. This verifies the outer bean type, not the complete nested chain by reflection. The full `booking-service` `clean verify` suite was reported green on 16 October 2026.

### What Bulkhead does not guarantee

Bulkhead does not replace an Event Service-side concurrency limit, a global request rate limit, an overall latency deadline, or atomic Event capacity reservation. The concurrency test checks a controlled local instance; a distributed load test and production tuning remain pending.

## Coverage and limitations

**Implemented/tested:** inter-service availability HTTP adapter, exception mappings, configured per-attempt timeouts, bounded Retry, Circuit Breaker protection, fast OPEN rejection, successful/failed HALF_OPEN transitions, ignored 404, Bulkhead concurrency/rejection/permit release, and outer Spring bean wiring. A manual cross-service GET has been exercised; resilience unit tests are not full distributed end-to-end fault tests.

**Not yet implemented or verified:**

- Measured expiration of connection/read timeouts and an **overall call deadline** across retries.
- Real-time automatic OPEN→HALF_OPEN timing test; refined backoff/jitter and selective HTTP 5xx/429 retry policy.
- Resilience observability (metrics, logging, distributed traces), production tuning and capacity limits across replicas. Bulkhead is implemented at the individual Booking Service instance, not as a distributed global limit.
- Durable/idempotent Event-capacity compensation, distributed consistency guarantees, and real two-service end-to-end integration tests. Basic synchronous capacity reservation/compensation is implemented, but not fault-tolerant.
- Reusable `docs/MICROSERVICES-CHECKLIST.md` after studying the remaining cross-service reliability concerns.

See [`ROADMAP.md`](ROADMAP.md#23-microservices-evolution) for current/pending implementation milestones. The synchronous best-effort reservation/compensation path is documented separately below; the remaining reliability gaps are not described as implemented.


## Current create-booking path — capacity reservation and compensation (16 October 2026)

**Important distinction:** the availability `GET /events/{id}` and its `Bulkhead → Circuit Breaker → Retry → REST` decorators remain implemented, but **`BookingService.createBooking()` no longer calls `EventAvailabilityPort.isAvailable()`**. Its current path is:

```text
BookingController → CompensatingBookingUseCase
  → EventCapacityPort.reservePlaces(eventId, 1)
    → RestEventCapacityAdapter → POST /events/{id}/reserve/1/places
  → TransactionalBookingUseCase → BookingService → PostgreSQL
  → on delegate failure: EventCapacityPort.releasePlaces(eventId, 1)
    → RestEventCapacityAdapter → POST /events/{id}/release/1/places
```

Both capacity calls use blocking `RestClient` without an automatic mutating-request retry decorator. This avoids blindly repeating a POST after a network timeout when its side effect may already have occurred. The reserve adapter translates 409 to `EventNotAvailableException`, 404 to `EventNotFoundException`, and upstream 5xx/transport failure to `EventServiceUnavailableException`. A **release 409 currently propagates** as `HttpClientErrorException.Conflict`; it is not equivalent to an unavailable event. The outer coordinator tries to release a previously reserved place if Booking creation fails and propagates the original creation error.

**Verified:** eight `RestEventCapacityAdapterTest` HTTP-mock tests and three `CompensatingBookingUseCaseTest` coordination tests are green. Event Service's insufficient-capacity WebTestClient test is green. **Not verified:** a live two-service booking/compensation flow and durable, idempotent recovery. If reserve or compensation times out, a remote side effect may already have happened; no distributed exactly-once guarantee or persistent compensation log exists. Booking cancellation does not yet release capacity in Event Service.

## Manual live-service failure evidence — 17 October 2026

With Event Service stopped, a real `POST /api/bookings` to Booking Service returned **503** with application code `EVENT_SERVICE_UNAVAILABLE` and a controlled message. This validates the observed handling of a stopped remote service for the **current mutating reservation path**. It does **not** demonstrate the GET-specific Circuit Breaker/Retry/Bulkhead decorators handling this POST, nor measured read/connect timeout expiry or an ambiguous result after remote execution. See [`MICROSERVICES_COMMUNICATION.md`](MICROSERVICES_COMMUNICATION.md) for the other manual E2E cases.
