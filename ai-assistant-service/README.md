# AI Assistant Service — EventHub Reactive

`ai-assistant-service` is an independent Spring Boot microservice in the **EventHub Reactive** portfolio project. It explores **Spring AI, Hexagonal Architecture, reactive HTTP, and AI integration** through an incremental, testable implementation.

**Current scope:** Prompt/response endpoints, OpenAI-backed and fake model adapters, and Server-Sent Events (SSE) streaming are implemented. **Tool Calling and integration with EventHub's Event and Booking services are planned, not implemented.**

## 🎯 Project Goal

Learn how to integrate Large Language Models (LLMs) into a Java backend while maintaining clean application boundaries. The project separates AI-provider infrastructure from application use cases, supports token-free local experimentation, and demonstrates the difference between synchronous model calls and streaming HTTP responses.

The service is **a learning and portfolio implementation**, not a production-ready conversational assistant. It does not currently retrieve events, check live capacity, create bookings, or access EventHub databases.

## 🚀 Tech Stack

| Area | Technologies |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 4.1.x |
| AI integration | Spring AI 2.x, `ChatClient`, OpenAI model starter |
| HTTP | Spring WebFlux, REST |
| Reactive | Project Reactor (`Mono`, `Flux`, `boundedElastic`) |
| Streaming | Server-Sent Events (`text/event-stream`) |
| Architecture | Hexagonal Architecture (ports and adapters) |
| Configuration | Spring `@Configuration`, `@Bean`, `@Profile` |
| Testing | JUnit 5, Mockito |
| Build | Maven / Maven Wrapper |

Spring AI versions and the selected model are controlled by `pom.xml` and application configuration; verify these files for the exact versions used in your checkout.

## 🏗️ Architecture Overview

The application layer owns the **inbound use case** and **outbound model port**. HTTP and Spring AI belong to infrastructure.

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
```

**Dependency direction:** `AiService` depends on `AiModelPort`, not on `SpringAiAdapter` or `ChatClient`. `AiConfiguration` wires concrete implementations at startup. The arrow from the controller into the use case represents the runtime call; the controller depends on the use-case interface.

### Package structure

```text
src/
├── main/
│   ├── java/com/rubenmarin/aiassistantservice/
│   │   ├── AiAssistantServiceApplication.java
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── in/AskAiUseCase.java
│   │   │   │   └── out/AiModelPort.java
│   │   │   └── service/AiService.java
│   │   └── infrastructure/
│   │       ├── adapter/
│   │       │   ├── in/
│   │       │   │   ├── AiController.java
│   │       │   │   └── AiRequest.java
│   │       │   └── out/
│   │       │       ├── FakeAiAdapter.java
│   │       │       └── SpringAiAdapter.java
│   │       └── config/AiConfiguration.java
│   └── resources/application.yaml
└── test/java/com/rubenmarin/aiassistantservice/
    ├── AiAssistantServiceApplicationTests.java
    └── application/service/AiServiceTest.java
```

A dedicated domain model has not been introduced: the current use case simply forwards prompts. Domain concepts will be added only when justified by behavior.

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
                   HTTP text/event-stream (SSE)
```

The real adapter uses `ChatClient.prompt().user(prompt).stream().content()`; the fake adapter emits fixed chunks using `Flux.just(...).delayElements(...)`.

`Flux<String>` may emit multiple text chunks. A chunk is **not necessarily a full word, sentence, or semantic unit**. Streaming responses do not, on their own, guarantee non-blocking behavior for every underlying library operation.

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
| SSE endpoint | ✅ | `POST /api/v1/ai/ask/stream` |
| Blocking-call isolation | ✅ | `Mono.fromCallable` + `boundedElastic` |
| Application unit test | ✅ | Mockito-based response and delegation check |
| Manual real-model smoke test | ✅ | Initial OpenAI response was received |
| Manual fake SSE smoke test | ✅ | HTTP 200, `text/event-stream`, multiple events |
| Tool Calling | ⬜ | Not yet implemented |
| Event/Booking service queries | ⬜ | No cross-service integration yet |

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

**Response:** A plain-text answer (returned through `Mono<String>`). In the `dev` profile, it is the configured fake text; in `prod`, it is generated by the OpenAI-backed adapter.

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
data:Spring AI

data:is a framework

data:for AI integration
```

The response uses `Content-Type: text/event-stream` and emits events as the `Flux` produces text. The fake adapter currently delays its elements to make streaming easier to observe.

**Current API limitations:** No authentication, authorization, rate limiting, request validation policy, or documented error contract is established yet. These endpoints should not be exposed publicly as-is.

## ⚙️ Running Locally

### Prerequisites

- JDK 21
- Maven or the included Maven Wrapper
- A local port available for the AI service (default: 8083)
- An OpenAI API key **only when using the real model** (subject to Spring AI auto-configuration requirements)

The service does **not** require Event, Booking, or Payment services to run for its current features.

### Profiles

`AiConfiguration` selects the implementation through Spring profiles:

| Profile | `AiModelPort` implementation | Behavior |
| --- | --- | --- |
| `dev` | `FakeAiAdapter` | Local, simulated replies; no model API calls |
| `prod` | `SpringAiAdapter` | Real OpenAI requests; billed by the provider |

Configure the active profile explicitly, for example with the environment variable `SPRING_PROFILES_ACTIVE=dev`. The name `prod` here means **real adapter selection**, not production readiness.

If neither expected profile is active, the `AiModelPort` bean may be missing and startup may fail. Do not enable both adapter profiles at once without a bean-selection rule.

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

Run with the Maven Wrapper:

```powershell
.\mvnw.cmd test
```

**Current evidence:** The application unit test was written and reviewed; the user manually verified local startup, an initial real LLM reply, and the fake SSE endpoint. This README does not claim a fresh full-suite execution or automated SSE/backpressure coverage.

### Testing still to implement

- WebFlux controller tests with `WebTestClient`
- Streaming tests with `StepVerifier`
- Adapter selection tests for Spring Profiles
- Fake-adapter contract tests
- Request validation, error handling, and cancellation behavior
- External-model integration tests with controlled cost
- Tool Calling contract tests once tools exist

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
| 8 | Tool Calling with a simple local tool | ⬜ |
| 9 | Event Service read-only queries | ⬜ |
| 10 | Booking Service read-only queries | ⬜ |
| 11 | Structured Output / typed responses | ⬜ |
| 12 | RAG / vector search exploration | ⬜ |
| 13 | Automated controller/adapter/integration tests | ⬜ |
| 14 | Authentication, authorization, resilience, observability | ⬜ |
| 15 | Review production deployment constraints | ⬜ |

**Next development milestone:** Introduce Spring AI Tool Calling using a small, read-only demonstration tool. Keep tool orchestration in infrastructure and business rules in the existing application services.

## 📚 Related Documentation

- [EventHub root README](../README.md)
- [EventHub roadmap](../docs/ROADMAP.md)
- [Spring AI reference documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring WebFlux documentation](https://docs.spring.io/spring-framework/reference/web/webflux.html)
- [Project Reactor reference](https://projectreactor.io/docs/core/release/reference/)

## 👨‍💻 Author

**Rubén Marín** — Senior Java Backend Developer.

This service is part of an ongoing learning and portfolio project focused on modern Java backend engineering, AI integration, reactive programming, software testing, and clean architecture.
