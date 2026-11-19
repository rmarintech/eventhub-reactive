# AI Assistant Service — EventHub Reactive

`ai-assistant-service` is an independent Spring Boot microservice in the **EventHub Reactive** portfolio project. It explores **Spring AI, Hexagonal Architecture, reactive HTTP, and AI integration** through an incremental, testable implementation.

**Current scope:** Prompt/response endpoints, OpenAI-backed and fake model adapters, **typed Server-Sent Events (SSE) with controlled provider-error events**, Spring AI Tool Calling, **read-only availability lookup through EventHub's real Event Service**, and automated adapter/controller tests have been implemented and exercised. Booking Service integration, additional AI tools, and production hardening are still planned.

## 🎯 Project Goal

Learn how to integrate Large Language Models (LLMs) into a Java backend while maintaining clean application boundaries. The project separates AI-provider infrastructure from application use cases, supports token-free local experimentation, and demonstrates the difference between synchronous model calls and streaming HTTP responses.

The service is **a learning and portfolio implementation**, not a production-ready conversational assistant. It can currently **retrieve available seats for a known event UUID** by calling Event Service's HTTP API; it cannot search for events by natural language, create or cancel bookings, or access the Event Service database directly.

## 🚀 Tech Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.x |
| AI integration | Spring AI 2.x, `ChatClient`, `@Tool`, `@ToolParam`, OpenAI model starter |
| HTTP | Spring WebFlux, REST, `WebClient` (Event Service outbound integration) |
| Reactive | Project Reactor (`Mono`, `Flux`, `boundedElastic`) |
| Streaming | Server-Sent Events (`text/event-stream`), typed `ServerSentEvent<String>` (`message` / `error`) |
| Architecture | Hexagonal Architecture (ports and adapters) |
| Configuration | Spring `@Configuration`, `@Bean`, `@Profile` |
| Testing | JUnit 5, Mockito, WireMock (standalone), Reactor Test / `StepVerifier`, `WebTestClient`, `@WebFluxTest` |
| Build | Maven / Maven Wrapper |

The Spring AI version reported during development is **2.0.1** (`spring-ai.version`); the selected model is configured in application properties. Verify `pom.xml` and the active configuration for your checkout.

## 🏗️ Architecture Overview

The application layer owns **inbound use cases** and **outbound ports**. HTTP clients/controllers and Spring AI belong to infrastructure. The availability flow now crosses the service boundary to the existing Event Service via `WebClient`; no new Event Service endpoint was required.

```text
HTTP client
   |
   | POST /api/v1/ai/ask
   | POST /api/v1/ai/ask/stream
   v
AiController                         Infrastructure / inbound adapter
   |
   v
AskAiUseCase                         Application / inbound port
   ^
   |
AiService                            Application / use-case implementation
   |
   v
AiModelPort                          Application / outbound port
   ^
   |
   +----------------------+---------------------+
   |                                            |
FakeAiAdapter                             SpringAiAdapter
Infrastructure / outbound                Infrastructure / outbound
   |                                            |
Fixed response / delayed Flux                 ChatClient
                                                |
                                                v
                                             OpenAI API
                                                |
                                    LLM requests getAvailableSeats
                                                |
                                                v
                                  EventTools (@Tool; infrastructure)
                                                |
                                                v
                                  GetAvailableSeatsUseCase
                                                |
                                                v
                                  EventAvailabilityService
                                                |
                                                v
                                  EventAvailabilityPort
                                                |
                                                v
                                RestEventAvailabilityAdapter
                                                |
                                          WebClient GET
                                                |
                                                v
                                 Event Service (:8080) /events/{id}
```

**Tool Calling execution:** OpenAI selects and supplies arguments for `getAvailableSeats`; Spring AI invokes the Java method, passes the returned availability into the next model prompt, and obtains the final answer. The LLM does not execute Java code itself. This tool is read-only. `FakeAiAdapter` remains useful for no-token experiments and can invoke `EventTools` explicitly; this is not equivalent to LLM-driven tool selection.

**Dependency direction:** `AiService` depends on `AiModelPort`, not on `SpringAiAdapter` or `ChatClient`. `AiConfiguration` and `EventConfiguration` wire concrete implementations at startup. The arrow from the controller into the use case represents the runtime call; the controller depends on the use-case interface.

### Package structure

```text
src/
├── main/
│   ├── java/com/rubenmarin/aiassistantservice/
│   │   ├── AiAssistantServiceApplication.java
│   │   ├── application/
│   │   │   ├── exception/
│   │   │   │   ├── EventNotFoundException.java
│   │   │   │   └── EventServiceUnavailableException.java
│   │   │   ├── port/
│   │   │   │   ├── in/
│   │   │   │   │   ├── AskAiUseCase.java
│   │   │   │   │   └── GetAvailableSeatsUseCase.java
│   │   │   │   └── out/
│   │   │   │       ├── AiModelPort.java
│   │   │   │       └── EventAvailabilityPort.java
│   │   │   └── service/
│   │   │       ├── AiService.java
│   │   │       └── EventAvailabilityService.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── exception/InvalidToolArgumentException.java
│   │       │   ├── in/
│   │       │   │   ├── AiController.java
│   │       │   │   └── AiRequest.java
│   │       │   └── out/
│   │       │       ├── ai/
│   │       │       │   ├── FakeAiAdapter.java
│   │       │       │   ├── SpringAiAdapter.java
│   │       │       │   ├── exception/AiProviderException.java
│   │       │       │   └── tool/EventTools.java
│   │       │       └── event/
│   │       │           ├── RestEventAvailabilityAdapter.java
│   │       │           └── EventAvailabilityResponse.java
│   │       └── config/
│   │           ├── AiConfiguration.java
│   │           └── EventConfiguration.java
│   └── resources/application.yaml
└── test/java/com/rubenmarin/aiassistantservice/
    ├── AiAssistantServiceApplicationTests.java
    ├── application/service/AiServiceTest.java
    └── infrastructure/adapter/
        ├── in/AiControllerTest.java
        └── out/
            ├── ai/SpringAiAdapterTest.java
            ├── ai/tool/EventToolsIT.java
            └── event/RestEventAvailabilityAdapterTest.java
```

A global WebFlux exception handler (`GlobalExceptionHandler`) is also used for controlled HTTP errors; its exact package is not asserted in this tree.

A dedicated domain model has not been introduced: the AI use cases coordinate prompts and event availability reads without reproducing Event Service business logic. Domain concepts will be added only when justified by behavior.

## 🔄 Request Processing

### Synchronous model response exposed as a reactive HTTP publisher

```text
Client → AiController → AskAiUseCase → AiService
                                         |
                                         v
                                     AiModelPort
                                         |
                                         v
                                     Selected adapter
                                         |
                                         v
                                    String response
                                         |
                                         v
                                Mono<String> → HTTP
```

The application port's `ask(String)` method returns a `String`. The controller wraps that blocking operation in:

```java
Mono.fromCallable(() -> askAiUseCase.ask(request.question()))
    .subscribeOn(Schedulers.boundedElastic());
```

This moves the synchronous model invocation off WebFlux event-loop threads. It **does not make the underlying OpenAI call non-blocking**. Under load, the bounded worker pool and its queue still impose resource and latency limits.

### Streaming model response

```text
Client → AiController → AskAiUseCase.stream(...)
                              |
                              v
                     AiService.stream(...)
                              |
                              v
                     AiModelPort.streamResponse(...)
                              |
                              v
                  FakeAiAdapter / SpringAiAdapter
                              |
                              v
                          Flux<String>
                              |
                              v
                 AiController: ServerSentEvent<String>
                              |
                              v
                   HTTP text/event-stream (SSE)
```

The real adapter uses `ChatClient.prompt().user(prompt).stream().content()`; the fake adapter emits fixed chunks using `Flux.just(...).delayElements(...)`.

`Flux<String>` may emit multiple text chunks. The controller maps each chunk to a typed SSE `message` event. A chunk is **not necessarily a full word, sentence, or semantic unit**. Streaming responses do not, on their own, guarantee non-blocking behavior for every underlying library operation.

For provider failures, `SpringAiAdapter.streamResponse()` maps `RateLimitException` to `AiProviderException` using `onErrorMap()`. The controller uses `switchOnFirst()` to distinguish an error **before the first signal** (handled as HTTP 503) from an error **after streaming has started** (sent as a typed SSE `error` event, then the stream completes). Once the HTTP response has been committed, its status cannot be changed. The first-signal distinction does not by itself guarantee a response has been committed: that depends on actual HTTP emission.

### Tool Calling and live availability

The `prod` profile registers `EventTools` with `ChatClient.defaultTools(eventTools)`. The tool accepts a **UUID string**, validates it, and calls `GetAvailableSeatsUseCase`, which delegates to `EventAvailabilityPort` and the `RestEventAvailabilityAdapter`.

```text
User: "How many seats are available for event <UUID>?"
    ↓
ChatClient → OpenAI LLM (tool definitions included)
    ↓ requested tool: getAvailableSeats(eventId=<UUID>)
Spring AI → EventTools
    ↓
GetAvailableSeatsUseCase → EventAvailabilityService
    ↓
EventAvailabilityPort → RestEventAvailabilityAdapter
    ↓ GET /events/{id}
Event Service → JSON field "available"
    ↓
Tool result → second LLM call → natural-language answer
```

The Event Service response uses the JSON property **`available`**, not `availableCapacity`; the infrastructure DTO maps the actual property to an `Integer`. The adapter returns `Mono<Integer>`. The tool is synchronous and currently bridges this publisher with `blockOptional(Duration.ofSeconds(10))`: **do not assume it is safe to run on a WebFlux event-loop thread**. The synchronous `/ask` controller uses `boundedElastic`; the streaming tool-execution context still requires dedicated verification.

Current adapter error translation: HTTP 404 → `EventNotFoundException`; HTTP 503 and request-level connectivity failures → `EventServiceUnavailableException`. The connection-failure mapping preserves the underlying cause. Other HTTP errors, empty availability values, timeout and tool-error messaging policies still need review.

## ✅ Implemented Features

| Capability | Status | Notes |
| --- | --- | --- |
| Standalone Spring Boot application | ✅ | Runs independently of other EventHub services |
| Hexagonal application ports and service | ✅ | `AskAiUseCase`, `AiService`, `AiModelPort` |
| Dependency wiring | ✅ | Explicit `@Bean` configuration |
| OpenAI-backed adapter | ✅ | `ChatClient` model calls and streaming |
| Fake model adapter | ✅ | Fixed strings and delayed chunks; no API tokens |
| Spring Profiles | ✅ | `dev` selects fake; `prod` selects real adapter |
| Synchronous endpoint | ✅ | `POST /api/v1/ai/ask` |
| SSE endpoint | ✅ | `POST /api/v1/ai/ask/stream`; typed `message` and `error` events |
| Blocking-call isolation | ✅ | `Mono.fromCallable` + `boundedElastic` |
| Application unit test | ✅ | Mockito-based response and delegation check |
| Manual real-model smoke test | ✅ | Initial OpenAI response was received |
| Manual fake SSE smoke test | ✅ | HTTP 200, `text/event-stream`, multiple events |
| LLM-driven Tool Calling | ✅ | `EventTools.getAvailableSeats()` via `ChatClient.defaultTools()` |
| Event Service read-only availability | ✅ | `WebClient` to `GET /events/{id}` using event UUID |
| Live model → tool → Event Service smoke test | ✅ | Real request and execution log confirmed |
| WireMock HTTP adapter tests | ✅ | 200, 404, 503 and connection failure scenarios |
| Mocked-model Tool Calling test | ✅ | Two prompts, registered tool, tool execution, result `25` in `ToolResponseMessage` |
| AI provider exception translation | ✅ | `RateLimitException` → `AiProviderException` for regular and streaming calls |
| HTTP error mapping | ✅ | `GlobalExceptionHandler` returns a controlled HTTP 503 for synchronous requests / early stream failures |
| Spring AI streaming tests | ✅ | Chunk emission and `Flux.error()` mapping with `StepVerifier` |
| WebFlux controller tests | ✅ | `WebTestClient` validates successful SSE events, early HTTP 503 and late typed `error` events |
| Live EventTools integration test | ✅ | Reads from running Event Service; environment-dependent |
| Booking Service tools / event search | ⬜ | Not implemented |

The manual SSE test used PowerShell `Invoke-WebRequest`, which displayed the assembled result. It verifies the SSE-formatted response, **not the observed arrival time of each chunk**.

## 🌐 HTTP API

The default local port used for this project is **8083**.

### 1. Ask — complete response

`POST /api/v1/ai/ask`

**Request**

```http
POST http://localhost:8083/api/v1/ai/ask
Content-Type: application/json
```

```json
{
  "question": "What is Spring AI?"
}
```

**Response:** A plain-text answer (returned through `Mono<String>`). When the `prod` profile is active, the model can request the available-seats tool for a valid EventHub event UUID. In the fake profile, any tool invocation is explicitly simulated rather than selected by an LLM.

**Real availability example (manual verification on 2026-11-16):**

```json
{
  "question": "How many seats are available for EventHub event 0ebbca7e-87bb-4c1d-915c-676b59ffe329?"
}
```

The model returned **999990 available seats**, and the AI Assistant logs confirmed that `EventTools` executed for that UUID. This is a historical test result, **not a guaranteed current value**.

### 2. Ask — streamed response

`POST /api/v1/ai/ask/stream`

**Request**

```http
POST http://localhost:8083/api/v1/ai/ask/stream
Content-Type: application/json
Accept: text/event-stream
```

```json
{
  "question": "Explain Spring AI briefly."
}
```

**Illustrative response**

```text
event:message
data:Spring AI

event:message
data:is a framework

event:message
data:for AI integration
```

The response uses `Content-Type: text/event-stream` and emits **`event: message`** events as the `Flux` produces text. The fake adapter currently delays its elements to make streaming easier to observe.

**Controlled late-stream error (illustrative):**

```text
event:message
data:Hello

event:error
data:AI service temporarily unavailable
```

If the AI provider fails before the first streamed element, the controller lets the exception propagate to `GlobalExceptionHandler`, which can return **HTTP 503**. If failure occurs after response streaming starts, an HTTP status change is no longer possible; the controller converts `AiProviderException` into an **SSE `error` event** containing a generic message, then completes the stream. This SSE `error` event is an application-level event, **not** an HTTP error status. A browser client should listen for named events and handle connection termination.

For a synchronous `/ask` provider failure, the global exception handler returns a controlled response such as:

```json
{
  "error": "AI_SERVICE_UNAVAILABLE",
  "message": "AI service temporarily unavailable"
}
```

This hides upstream billing/rate-limit details from API clients; internal logs retain the original cause as appropriate.

**Current API limitations:** Basic provider-error handling is implemented, but comprehensive error contracts (for validation, downstream service failures and every stream termination), authentication, authorization, rate limiting and request validation are not established. These endpoints should not be exposed publicly as-is.

## ⚙️ Running Locally

### Prerequisites

- JDK 21
- Maven or the included Maven Wrapper
- A local port available for the AI service (default: 8083)
- An OpenAI API key **only when using the real model** (subject to Spring AI auto-configuration requirements)

Basic fake responses and generic model prompts can run independently. **Live event availability queries require Event Service on its configured base URL** (locally `http://localhost:8080`) and an existing event UUID. Booking and Payment services are not needed for the current AI features.

### Profiles

`AiConfiguration` selects the implementation through Spring profiles:

| Profile | `AiModelPort` implementation | Behavior |
| --- | --- | --- |
| `dev` or any profile set without `prod` | `FakeAiAdapter` | Local, simulated replies; no model API calls |
| `prod` | `SpringAiAdapter` | Real OpenAI requests; billed by the provider |

Configure the active profile explicitly, for example with the environment variable `SPRING_PROFILES_ACTIVE=dev`. The name `prod` here means **real adapter selection**, not production readiness.

Current bean wiring uses `@Profile("!prod")` for the fake adapter and `@Profile("prod")` for the OpenAI-backed adapter. **No active profile selects the fake adapter.** Setting `prod` selects the real adapter even if another profile is active; using `prod` therefore consumes API tokens when a model request is made.

### OpenAI model selection

For Spring AI **2.0.1**, the project can choose a model via the `spring.ai.openai.chat.model` property. Example (merge into your existing `spring` configuration):

```yaml
spring:
  ai:
    openai:
      chat:
        model: ${OPENAI_MODEL:gpt-5-nano}
```

Model availability, pricing and token usage vary over time. A model setting does not grant API credits; ChatGPT subscriptions and API billing are separate. This README does not claim that per-request dollar cost tracking or durable accounting has been implemented.

### Event Service URL

Configure the downstream service address (merge into your existing `application.yaml`):

```yaml
eventhub:
  event-service:
    base-url: http://localhost:8080
```

`EventConfiguration` creates a `WebClient` with this base URL and injects it into `RestEventAvailabilityAdapter`; the adapter calls the relative path `/events/{id}`. In another deployment environment, override this URL with the appropriate service address.

### Local secrets

A local `.env` file may contain:

```dotenv
OPENAI_API_KEY=replace-with-your-real-key
```

Keep `.env` ignored by Git. Commit only a redacted `.env.example`:

```dotenv
OPENAI_API_KEY=your-api-key-here
```

Spring Boot does **not automatically load `.env` files**. If using Spring Config Data imports, the configuration can include:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

Merge this into the existing `spring` section of `application.yaml`. The relative path depends on the process working directory; IntelliJ must run from the directory where `.env` is located. Alternatively, provide `OPENAI_API_KEY` directly through the operating-system environment or IntelliJ Run Configuration.

**Security:** Never commit real keys, print them in logs, or put them in HTTP examples. A fake adapter does not necessarily disable OpenAI model auto-configuration: model-starter initialization and credential requirements must be verified independently.

### Start the service

In Windows PowerShell, from the microservice directory:

```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
.\mvnw.cmd spring-boot:run
```

For the real adapter, set `SPRING_PROFILES_ACTIVE=prod` and configure the key securely before launching.

### Smoke test — PowerShell

```powershell
$body = @{ question = "Hello" } | ConvertTo-Json

Invoke-WebRequest `
  -Uri "http://localhost:8083/api/v1/ai/ask/stream" `
  -Method POST `
  -ContentType "application/json" `
  -Headers @{ Accept = "text/event-stream" } `
  -Body $body
```

`Invoke-WebRequest` may buffer the output. To measure progressive delivery, use an SSE-capable streaming client.

## 🧪 Testing

### Application unit test

`AiServiceTest` uses Mockito to mock `AiModelPort` and verify that:

- The service delegates the received question.
- It returns the configured response.
- The model port method is invoked once with the correct question.

The test runs without Spring ApplicationContext, an OpenAI key, network access, or token costs.

Run unit tests with the Maven Wrapper:

```powershell
.\mvnw.cmd test
```

If Maven Failsafe is configured in `pom.xml`, integration tests named `*IT` run during `verify`:

```powershell
.\mvnw.cmd verify
```

`EventToolsIT` requires the real Event Service running with the expected data; it is not suitable for an isolated CI pipeline until the upstream service and test data are provisioned as part of the test setup.

### Tool Calling, SSE and Event Service tests

- **`SpringAiAdapterTest` (mocked `ChatModel`):** supplies a simulated tool-call `ChatResponse` and a subsequent final response. A Mockito spy verifies `EventTools.getAvailableSeats()` was called, and the mocked use case returns `Mono.just(25)`. Captured prompts verify that the tool is registered and the second prompt includes a `ToolResponseMessage` with `responseData()` equal to `"25"`. Also verifies that `generateResponse()` maps a mocked OpenAI `RateLimitException` to `AiProviderException`, preserving the cause. For streaming, `StepVerifier` checks multiple text chunks, stream completion, and reactive exception mapping via `Flux.error()`. No OpenAI API usage.
- **`AiControllerTest` (`@WebFluxTest`, `WebTestClient`, mocked `AskAiUseCase`):** verifies that the synchronous endpoint maps `AiProviderException` to HTTP 503 with a generic error body. SSE tests validate HTTP 200, `text/event-stream`, named `message` events and their payloads; an early provider error produces HTTP 503; a later provider error becomes a named SSE `error` event followed by completion. A `Sinks.Many` test explicitly emits the error after the first received event, avoiding timing-based sleeps. No OpenAI API usage.
- **`RestEventAvailabilityAdapterTest` (WireMock + `StepVerifier`):** verifies the real `WebClient` path, JSON property `available`, and 200/404/503/connection-failure handling. No real Event Service or OpenAI required.
- **`EventToolsIT` (live Spring context):** invokes `EventTools` against the locally running Event Service and asserts that returned availability is nonnegative. **This is an environment-dependent smoke/integration test**, not a deterministic repository-only test. It requires an existing UUID in Event Service.
- **Manual real-model tool-call smoke test (2026-11-16):** logs confirmed invocation of `EventTools` for `0ebbca7e-87bb-4c1d-915c-676b59ffe329` and retrieval of `999990` seats. This manual test consumed OpenAI tokens.

The reported tests were individually green during development; this README does **not** claim a fresh complete Maven verification run.

### Testing still to implement

- Adapter selection tests for Spring Profiles
- Fake-adapter contract tests
- Request validation, additional provider/downstream error cases, and client cancellation / resource cleanup
- External-model integration tests with controlled cost and explicit opt-in
- Deterministic cross-service integration tests (without relying on a specific live event UUID)
- Tool-level invalid UUID, not-found, empty result and timeout/error behavior tests
- Tool Calling with streaming, including verification of blocking behavior and scheduler context

## 🔐 Reliability and Security Considerations

The current service deliberately has a small functional surface. Before public exposure, it will need access control, prompt/input validation, appropriate timeouts, rate limits, spend controls, logging policies that avoid sensitive prompts, error handling, and request/response observability.

For any future **state-changing tool** (such as canceling a booking), require explicit user confirmation and enforce server-side authorization and business rules. An LLM's proposed tool arguments are untrusted input.

## 🗺️ Roadmap

| Phase | Milestone | Status |
| --- | --- | --- |
| 1 | Standalone service + hexagonal wiring | ✅ |
| 2 | ChatModel / ChatClient fundamentals | ✅ Studied |
| 3 | OpenAI-backed prompt/response | ✅ |
| 4 | Reactive wrapper for blocking calls | ✅ |
| 5 | SSE streaming | ✅ |
| 6 | Fake adapter + profile selection | ✅ |
| 7 | Application unit tests | ✅ Initial test |
| 8 | Tool Calling with a simulated tool and mocked model | ✅ |
| 9 | Event Service read-only availability query | ✅ |
| 10 | Booking Service read-only queries | ⬜ |
| 11 | Structured Output / typed responses | ⬜ |
| 12 | RAG / vector search exploration | ⬜ |
| 13 | Adapter tests (WireMock); WebFlux/SSE/controller error tests | ✅ Core scenarios; more coverage pending |
| 14 | Authentication, authorization, resilience, observability | 🚧 Provider error handling in place; other work pending |
| 15 | Review production deployment constraints | ⬜ |

**Next development milestone:** Verify cancellation and resource cleanup when an SSE client disconnects, then harden the availability tool for invalid UUIDs, nonexistent events, upstream outages and empty responses. Review how Spring AI communicates safe tool errors to the model. Later, add further read-only EventHub queries where useful. Business rules remain in the existing services.

## 📚 Related Documentation

- [EventHub root README](../README.md)
- [EventHub roadmap](../docs/ROADMAP.md)
- [Spring AI reference documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring WebFlux documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor reference](https://projectreactor.io/docs/core/release/reference/)

## 👨‍💻 Author

**Rubén Marín** — Senior Java Backend Developer.

This service is part of an ongoing learning and portfolio project focused on modern Java backend engineering, AI integration, reactive programming, software testing, and clean architecture.
