# Microservices Communication — Practical Guide and Testing Checklist

> **Purpose.** A reusable guide for designing, implementing, and **demonstrating through tests** reliable communication between Java/Spring Boot microservices. The running example is **EventHub**: **Event Service** (WebFlux + R2DBC, `:8080`) and **Booking Service** (Spring MVC + JPA, `:8081`). This is not a list of technologies that every project must install: choose the guarantees required by the business.
>
> **Evidence rule:** *implemented* does not mean *tested*; a mocked HTTP server is not an end-to-end test; a 200/201 response alone does not prove distributed consistency.

## 1. The business flow we want to guarantee

```text
Client
  | POST /api/bookings { eventId, userId }
  v
BookingController (Booking Service :8081)
  | BookingUseCase (@Primary: CompensatingBookingUseCase)
  | POST /events/{id}/reserve/1/places
  v
EventController (Event Service :8080)
  | ReserveEventPlacesService -> Event.reservePlaces(1)
  | R2DBC + optimistic locking -> Event PostgreSQL
  | HTTP 2xx or 4xx/5xx
  v
CompensatingBookingUseCase
  | if reservation succeeds: TransactionalBookingUseCase -> BookingService
  |                          -> JPA -> Booking PostgreSQL -> COMMIT
  | if creation/commit fails: POST /events/{id}/release/1/places
  v
HTTP 201 / Booking Service error
```

**Target business invariant:** successfully creating a Booking consumes **exactly one place**. If local Booking creation fails after a confirmed remote reservation, the service attempts to release that place. **Current status:** synchronous compensation is implemented and tested with mocks; basic manual two-service E2E scenarios were exercised. Full post-reservation failure injection and durable recovery remain pending. There is no shared ACID transaction across the two PostgreSQL databases.

### Essential terminology

- **API contract:** the explicit agreement about paths, HTTP methods, DTO schemas, status codes, and error semantics. OpenAPI helps document and review the agreement.
- **Port:** an application interface independent of HTTP (`EventCapacityPort`). **Adapter:** an infrastructure implementation using `RestClient` (`RestEventCapacityAdapter`).
- **Business error:** a technically valid request cannot be fulfilled (`409` for insufficient capacity). **Technical error:** unavailable service, `5xx`, connection failure, timeout.
- **Local transaction:** atomic changes in *one* database through `@Transactional`. It does not automatically include remote HTTP calls.
- **Compensation / Saga:** a later business operation attempts to undo an already committed effect in another service. It is not an automatic distributed `ROLLBACK`.
- **Idempotency:** repeating the *same logical operation* does not apply its effects twice. The operation must be identifiable; checking only whether an active Booking already exists is insufficient.
- **Ambiguous outcome:** the caller did not receive confirmation (for example, a timeout), but the remote service **may already have reserved capacity**.
- **Distributed tracing:** correlating logs and spans belonging to one operation across service boundaries.

## 2. Reusable matrix: implementation, tests, acceptance

**EventHub status legend:** ✅ tested within the stated scope; 🟡 partly implemented/tested; ⬜ pending or not evidenced. Statuses reflect **the tests reviewed so far**, not a production environment.

| # | Area | What to implement/configure | How to demonstrate it | EventHub status |
|---|---|---|---|---|
| 01 | API contract | Paths, methods, schema, 2xx/4xx/5xx, compatible versioning | Contract tests and validation of real requests/responses | 🟡 HTTP tested separately; cross-service contract pending |
| 02 | HTTP client | Environment-specific base URL, RestClient, DTOs, headers, settings | `MockRestServiceServer` checks URL, POST and response; then real service-to-service test | ✅ mock + basic manual E2E |
| 03 | Happy path | Remote capacity reservation followed by Booking commit | Create a Booking and inspect both states: Booking exists, available places decrease by 1 | ✅ isolated + basic manual E2E |
| 04 | Business errors | Translate 404/409 into application errors | Missing event / insufficient capacity; no Booking or accidental capacity change | ✅ mock + basic manual E2E |
| 05 | Technical errors | Translate 5xx and I/O failures; do not leak stack traces | Simulate 500 and connection refused; verify Booking returns 503 | ✅ mock + basic manual E2E |
| 06 | Timeouts | Connect/read timeouts; optional overall deadline | Inject controlled delay, measure bound, investigate ambiguous remote outcome | 🟡 configured, actual expiration not tested |
| 07 | Retry | Retry only when repetition is safe or idempotent | Transient failure, maximum attempts, no retry for 404 | ✅ availability GET; **not** mutating POST |
| 08 | Circuit Breaker | Temporarily stop calls to repeatedly failing dependency | CLOSED→OPEN→HALF_OPEN→CLOSED; fast rejection | ✅ availability GET; not POST |
| 09 | Bulkhead | Limit concurrent calls per instance | Saturation, rejection, permit recovery | ✅ availability GET; not POST |
| 10 | Local transactions | `@Transactional`, database constraints, flush/commit semantics | Persistence, rollback, real duplicates with Testcontainers | ✅ Booking JPA |
| 11 | Compensation | `reserve` → create Booking → `release` on delegate failure | Mockito verifies release request; E2E verifies actual restored capacity | ✅ unit + duplicate scenario manual E2E; injected failure pending |
| 12 | Idempotency | `operationId` and durable deduplication/result tracking | Repeat an operation or lose a response without double decrement | ⬜ |
| 13 | Concurrency | Optimistic locking/Event version and DB constraints | Two requests compete for the last place; one succeeds, no overselling | 🟡 isolated tests; stronger E2E evidence pending |
| 14 | Failed compensation | Durable operation log and reconciliation | Make Event unavailable during `release`, restore it, reconcile | ⬜ best-effort handling only |
| 15 | Observability | Correlation/trace IDs, structured logs, metrics | Follow the same operation across services and locate failures | ⬜ |
| 16 | Security | Service identity, TLS as applicable, scopes/authorization | Missing/expired token, 401/403, correct permissions | ⬜ |
| 17 | Real E2E | Run both services with separate PostgreSQL databases | Execute P0 cases below with no mocked capacity port | 🟡 manual local two-service scenarios ✅; automation pending |
| 18 | Deployment | DNS/service discovery, configuration, health/readiness, restarts | Restart/lose an instance; demonstrate detection and recovery | ⬜ |

**Rule for checking ✅:** record the test, its result, and **its scope** (unit, mocked HTTP, real database, or real E2E). A green adapter test cannot replace a combined system test.

## 3. Executable EventHub plan — priorities

Each scenario generates its **own `eventId` and `userId`**, avoiding test-order dependencies. Initial capacity is explicitly controlled. Inspect both systems through their APIs; if an API cannot expose the necessary state, use a read-only SQL query against test data. HTTP status alone is not sufficient.

| Priority | Scenario | Setup and action | Expected assertions / acceptance evidence |
|---|---|---|---|
| P0 | Successful creation | Create and publish Event with 3 places; POST Booking for new user | 201; Booking `PENDING`; Event available 3→2; exactly one Booking row |
| P0 | Last available place | Published Event with 1 place; create Booking | 201; available capacity = 0 |
| P0 | No capacity | Published Event with 1 place: request 2 directly from Event **or** consume its last place and create another Booking | Event: insufficient-capacity 409; Booking: 409 `EVENT_NOT_AVAILABLE`; no extra Booking; capacity never negative |
| P0 | Missing event | POST Booking with nonexistent UUID `eventId` | 404 `EVENT_NOT_FOUND`; no new Booking |
| P0 | Duplicate Booking | Create Booking; repeat same `(eventId,userId)` | Second POST 409 `BOOKING_ALREADY_EXISTS`; one active Booking; capacity reduced **only once overall** (second reservation compensated) |
| P0 | Booking persistence failure | Inject **controlled** failure after remote reservation, before/during JPA commit | Original error; no persisted Booking; Event capacity restored by remote `release` |
| P0 | Event responds 500 | Make Event return 500 during reservation | Booking responds 503; no Booking; verify remote state before claiming nothing happened |
| P1 | Event down/slow | Stop the service or add delay exceeding client timeout | Controlled failure and bounded latency; determine whether POST result is ambiguous |
| P1 | Race for last place | Event with 1 place; two distinct users send POST concurrently | Exactly one successful Booking; other request rejected; final availability 0; repeat multiple times |
| P1 | Compensation failure | Trigger local Booking failure, and make remote `release` fail | Original failure remains visible; document possibly leaked capacity and recovery strategy |
| P1 | Lost reservation response | Event processes `reserve`, but Booking never receives the response | Demonstrate ambiguity; **do not** assume Event did not reserve |
| P2 | Idempotent retry | Implement `operationId`; submit the same logical request twice | No duplicate Booking or second capacity decrement |
| P2 | Crash between steps | Kill Booking after reservation and before persistence/compensation | Detect and recover incomplete operation using durable state |
| P2 | Tracing | Instrument both services | Same trace/correlation ID across the entire operation |

### Manual walkthrough P0.1 — happy path

1. Start both test databases and both services. Check that Event answers on `:8080` and Booking on `:8081`. Ensure the Booking `RestClient` base URL points to the actual Event Service. **Inside Docker, `localhost` refers to the same container**, not the other service.
2. Create an Event with `total=3`, record its ID, then **publish** it using the existing `POST /events` and `POST /events/{id}/publish` endpoints with the project's actual request DTO.
3. Fetch `GET /events/{id}` and record `available=3` as the baseline.
4. Send `POST http://localhost:8081/api/bookings` with JSON `{"eventId":"<EVENT_UUID>","userId":"<USER_UUID>"}` and `Content-Type: application/json`.
5. Assert HTTP **201** and response fields `eventId`, `userId`, and `status=PENDING`. Record the Booking ID when returned.
6. Fetch `GET /events/{id}` again; `available` must be **2**. Verify that Booking exists through its GET endpoint and/or a read-only database query.
7. Save the evidence: request/response, initial/final Event state, persisted Booking, test environment, and commit hash. **Only then mark P0.1 as passed.**

### Manual walkthrough P0.5 — duplicate and compensation

With a Booking already created, repeat the POST with the **same** `eventId` and `userId`. Expect `409 BOOKING_ALREADY_EXISTS`; the database must still contain one active Booking, and Event's final availability must be the same as before the second POST. The current orchestration reserves before detecting the duplicate, so the second reservation is expected to be **compensated by a release**. This is **not** a general-purpose idempotency guarantee.

### Manual walkthrough P0.6 — persistence failure after reservation

Do not confuse Event Service being down **before** reservation with a failure **after** reservation. For an E2E compensation test, `reservePlaces()` must succeed first; only **then** inject a failure during local Booking creation, preferably via a test-only fault injection mechanism disabled in production. Read Event state before and after and verify the Booking transaction rolled back. A Mockito verification of `.releasePlaces(...)` **does not prove** that Event's PostgreSQL capacity was restored.

## 4. How to test each layer in Java/Spring

### A. Unit test (no Spring context)

**Goal:** verify business rules and orchestration; **not** HTTP, JPA, or actual commit. In `CompensatingBookingUseCaseTest`, mock the inner `BookingUseCase` and `EventCapacityPort`:

```java
when(transactionalDelegate.createBooking(eventId, userId))
        .thenThrow(new IllegalStateException("Simulated failure"));

assertThrows(IllegalStateException.class,
        () -> compensatingUseCase.createBooking(eventId, userId));

verify(eventCapacityPort).reservePlaces(eventId, 1);
verify(eventCapacityPort).releasePlaces(eventId, 1);
```

**Interpretation:** this proves the coordinator **requests compensation**; it does not prove the remote Event regained a place.

### B. HTTP adapter test (mock HTTP server)

**Goal:** check the HTTP contract and error mapping without starting Event Service. In `RestEventCapacityAdapterTest`, bind `MockRestServiceServer` to the same `RestClient.Builder` used to create the adapter:

```java
mockServer.expect(requestTo(
        "http://localhost:8080/events/" + eventId.eventId() + "/reserve/1/places"))
    .andExpect(method(HttpMethod.POST))
    .andRespond(withStatus(HttpStatus.CONFLICT));

assertThrows(EventNotAvailableException.class,
        () -> adapter.reservePlaces(eventId, 1));
mockServer.verify();
```

**Covered scenarios:** `reserve`: 200, 404, 409, 500; `release`: 200, 404, 409, 500. **Semantic distinction:** the current `release` 409 propagates as `HttpClientErrorException.Conflict`, *not* `EventNotAvailableException`. Introduce a release-specific mapping only if the contract requires it.

### C. Persistence/transaction integration test (real test database)

**Goal:** verify real commits, rollbacks, and DB uniqueness constraints. Use `@SpringBootTest`, PostgreSQL Testcontainers, and `@ServiceConnection`. Mock `EventCapacityPort` only when the test concerns Booking persistence in isolation; query `BookingRepository` **after** the operation has returned. `TransactionalBookingRollbackIT` injects a delegate that saves and then throws an exception: the row must not remain persisted.

**Important:** `saveAndFlush()` flushes SQL and exposes some constraint violations early, but **flush is not commit**. A successful database transaction must finish before treating Booking persistence as confirmed.

### D. Real service-to-service integration test

**Goal:** prove both processes actually communicate and persist the expected effects. Start both services with test databases, **without `@MockitoBean EventCapacityPort`**, and execute P0.1. Inspect state on both sides. Automate later using a reproducible environment; an initial manual run already reveals configuration, URL, port, or serialization mistakes.

### E. Failure injection and concurrency test

**Goal:** expose bugs that do not occur in the happy path. Use a controlled HTTP server to delay or sever connections; for concurrency use `CountDownLatch` to release two workers simultaneously, then repeat the scenario multiple times. A start barrier **does not guarantee** both threads passed the same availability precheck before writing; use a test-only internal barrier or deterministic race design if that exact interleaving must be proven.

**Reactor caveat:** `Mono.zip(a,b)` can cancel the other publisher when either fails. It does not prove both competing operations finished. Capture success/error for **each** operation independently and wait for both results before checking the final state.

## 5. Resilience decisions: when to use each mechanism

| Mechanism | What it solves | How to verify | Important limitation |
|---|---|---|---|
| Timeout | Prevents indefinite blocking | Inject latency; measure elapsed time and outcome | Timeout does not mean remote rollback |
| Retry | Recovers from transient failures | Assert attempts, recovery, and exhausted budget | **Do not retry mutating POSTs without idempotency** |
| Circuit Breaker | Stops repeated calls to an unhealthy dependency | OPEN/HALF_OPEN tests; ensure rejected call does not reach HTTP | Choose thresholds based on traffic and error types |
| Bulkhead | Limits simultaneous work and isolates saturation | Hold one permit; verify another call is rejected; release permit | Limits are per instance unless coordinated globally |
| Optimistic locking | Detects conflicting updates to the same version | Two conflicting writes; one loses and retries/receives conflict | Does not ensure consistency across separate databases |
| Compensation | Reverses an already committed business effect | Inject downstream failure; observe a real `release` | Compensation itself can fail; recovery policy needed |
| Idempotency key | Prevents reapplying the same intent | Repeat with same operation ID; check single effect | Requires persistent identity and defined replay response |

In EventHub, the legacy availability **GET** uses `Bulkhead → Circuit Breaker → Retry → REST`. It remains implemented for learning, but **`createBooking()` no longer uses it**. Mutating `POST reserve/release` calls have no automatic retry because a repeated request may apply the effect twice. See [RESILIENCE.md](RESILIENCE.md).

## 6. Distributed consistency: four failure moments

| Failure moment | What we know | Compensate? | Remaining risk |
|---|---|---|---|
| Failure **before sending** the reserve POST | No remote operation if it is certain the request was never sent | No | Distinguish from failure during transmission |
| Event returns **409** (confirmed rejection) | Reservation was not accepted due to capacity | No | Consistent error contract |
| Event returns **2xx**, then Booking commit fails | Remote reservation confirmed; Booking not committed | Yes: `release` | The release may also fail |
| Booking **loses the response** to reserve POST | Unknown whether Event reserved capacity | Do not decide blindly | Idempotency and reconciliation |

### Possible future evolution (not implemented)

A stronger approach may use a uniquely identifiable capacity reservation (`reservationId`/`operationId`), idempotent state transitions, durable step records, a reconciliation worker, and observability. Kafka/transactional outbox could support a later asynchronous design, **but Kafka alone cannot make two separate databases atomic**. We should not add infrastructure solely to tick every checklist box.

## 7. Minimum security and observability

**Security:** in a secured system, services must authenticate themselves (for example OAuth2 client credentials, mTLS, or another appropriate scheme), use TLS where required, authorize individual operations, and avoid hardcoded secrets. Test a valid, missing, and expired token; insufficient scopes (401/403 depending on the contract); and rotation/revocation if applicable.

**Observability:** propagate a correlation ID or W3C tracing context (`traceparent`); record `eventId`, `bookingId`/`operationId` without leaking sensitive data; collect latency and error metrics. Acceptance check: a Booking creation on `:8081` can be followed through Event Service on `:8080` using the same identifier, and a compensation can be recognized as part of the same logical operation.

## 8. Reusable acceptance evidence template

Copy this form for each scenario and fill it in **after actually running the test**:

```text
Scenario: P0.1 – Create Booking
Date/commit: __________________________
Environment and versions: _____________
Scope: [ ] Unit  [ ] Mock HTTP  [ ] Real DB  [ ] Real E2E
Initial state: ________________________
Action/request: _______________________
HTTP status and response: _____________
Final state, service A: _______________
Final state, service B: _______________
Supporting test/logs/trace: ___________
Result: [ ] PASS  [ ] FAIL  [ ] NOT RUN
Limitations/open issue: _______________
```

**Definition of Done for microservice communication:** an explicit API contract; verified E2E happy path; exercised business and technical errors; confirmed effects on **both systems**; evaluated concurrency; intentional timeout and ambiguous-result policy; sufficient monitoring to investigate incidents; security appropriate to the risk. Advanced mechanisms (idempotency, durable recovery, Circuit Breaker, and so on) are introduced when required, not as architectural decoration.

## 9. EventHub progress and next iteration

**Implemented and tested in isolation:** Event HTTP reserve/release with domain guards and HTTP 409; `RestEventCapacityAdapterTest` (8 cases); `CompensatingBookingUseCaseTest` (3 cases); Booking HTTP/JPA tests updated for `EventCapacityPort`; local transactions and partial unique index; availability **GET** resilience tested separately.

**Next incremental task:** Kafka failure handling and integration testing. The basic real-service scenarios have been manually recorded below; the remaining failure injection/concurrency scenarios stay pending. Keep 🟡/⬜ until verified. The historical Reactor concurrency test has a `Mono.zip` cancellation limitation; a green result does not prove both flows finished independently.

**Related docs:** [DDD.md](DDD.md) (domain and decorators), [REACTIVE.md](REACTIVE.md) (WebFlux), [RESILIENCE.md](RESILIENCE.md) (GET and Resilience4j), [ROADMAP.md](ROADMAP.md) (milestones).

## 10. Manual two-service E2E verification — 

**Environment:** running Event Service (`:8080`) and Booking Service (`:8081`), with their real PostgreSQL-backed state; requests made manually over HTTP, **without mocks**. These are manual observations, not an automated E2E suite.

| Scenario | Evidence observed | Outcome |
| --- | --- | --- |
| Successful Booking | `POST /api/bookings` returned **201 PENDING**; Event `8ee230f8-da77-46f2-a0d4-90ad7fbe2f71` changed `available: 20 → 19` | **PASS** |
| Duplicate Booking / compensation | Same event and user `eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee`: second POST returned **409 `BOOKING_ALREADY_EXISTS`**; subsequent Event GET remained `available: 19` | **PASS — restored final capacity** |
| Missing Event | Event `11111111-1111-4111-8111-111111111111`: Booking returned **404 `EVENT_NOT_FOUND`** | **PASS — HTTP translation** |
| Capacity exhausted | Event `b50aad29-b3a4-4179-877a-25eeadd4ac40` (`capacity: 1`): attempted additional Booking returned **409 `EVENT_NOT_AVAILABLE`**; Event GET remained `available: 0` | **PASS — HTTP and capacity** |
| Event Service stopped | Booking returned **503 `EVENT_SERVICE_UNAVAILABLE`** | **PASS — controlled connection failure** |

**Evidence boundaries:** responses and Event GET reads establish these observed outcomes. Independent SQL inspection of Booking rows, proof of the intermediate `19 → 18 → 19` sequence, a controlled **JPA failure after confirmed reservation**, and automatic replayable E2E tests were **not** performed. The duplicate scenario is consistent with the implemented compensation flow, but does not prove it under every failure condition.

**Further Kafka work:** broker-backed automated integration tests and failure handling; then integrate an asynchronous `BookingCreated` notification flow. **Keep synchronous REST for capacity reservation.** Deferred hardening: idempotency, ambiguous timeouts, durable compensation/reconciliation, deliberate race testing, real timeout measurements, and distributed tracing.
