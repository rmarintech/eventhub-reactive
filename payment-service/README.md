# 💳 EventHub — Payment Service

**Payment Service** es el microservicio de pagos de **EventHub Reactive**, una plataforma distribuida de gestión de eventos y reservas. Su dominio y persistencia están implementados; el procesamiento asíncrono de pagos todavía está en desarrollo.

El objetivo es incorporar un tercer *bounded context* y completar el flujo de negocio: **crear evento → solicitar reserva → solicitar pago → confirmar pago → confirmar reserva**. Booking Service actuará como **Saga Orchestrator**; Payment Service gestionará sus propios pagos y publicará el resultado mediante Kafka.

> **Estado:** dominio, persistencia, pruebas e idempotencia secuencial implementados. Kafka, Inbox, Outbox y Saga están pendientes. La ejecución del pago será inicialmente simulada, sin pasarela de pagos real.

## Tecnologías: implementadas y previstas

| Área | Tecnología |
|---|---|
| Plataforma | Java 21, Spring Boot 4.1, Maven |
| Arquitectura | DDD, Hexagonal Architecture |
| Aplicación | Spring MVC |
| Persistencia | Spring Data JPA, Hibernate, PostgreSQL, Flyway |
| Mensajería (pendiente) | Apache Kafka, Spring Kafka |
| Fiabilidad | Payment Idempotency secuencial implementada; Transactional Outbox e Idempotent Inbox pendientes |
| Pruebas | JUnit, Mockito, Testcontainers |
| Infraestructura | Docker Compose |

## Funcionalidad implementada

- Modelo de dominio `Payment`: estados `PENDING`, `COMPLETED` y `FAILED`, transiciones protegidas y Value Objects (`PaymentId`, `PaymentRequestId`, `BookingId`, `Money`).
- `CreatePaymentUseCase` y `CreatePaymentService`, con idempotencia secuencial por `PaymentRequestId` y detección de conflictos si cambian los datos.
- Persistencia con Spring Data JPA/Hibernate, PostgreSQL y migraciones Flyway. La columna `payment_request_id` tiene restricción `UNIQUE`.
- Unit tests e integration tests con JUnit, Mockito y Testcontainers.

**Limitación conocida:** dos solicitudes simultáneas con el mismo `PaymentRequestId` pueden competir por el `INSERT`. PostgreSQL impide crear pagos duplicados, pero una petición puede fallar con `DataIntegrityViolationException` en vez de recuperar el pago existente. El test del comportamiento concurrente deseado está deshabilitado y documentado.

## Responsabilidades previstas

Payment Service recibirá solicitudes `PaymentRequested`, registrará y procesará pagos simulados, y emitirá `PaymentCompleted` o `PaymentFailed`. No modificará reservas ni accederá a bases de datos de otros microservicios.

La integración será asíncrona y se diseñará para tolerar mensajes duplicados y fallos parciales, **sin prometer exactly-once** entre servicios.

## Documentación

- 🏗️ **[Arquitectura y decisiones técnicas](docs/ARCHITECTURE.md)**: flujo de la Saga, estados, Kafka, Inbox/Outbox, idempotencia, compensaciones y pruebas.
- 🗺️ **[Roadmap](docs/ROADMAP.md)**: fases, tareas y estado de avance.

## Estructura del módulo

```text
payment-service/
├── README.md
├── docs/
│   ├── ARCHITECTURE.md
│   └── ROADMAP.md
├── pom.xml
└── src/
    ├── main/
    └── test/
```

El siguiente incremento previsto es implementar `Idempotent Inbox`, procesar `PaymentRequested` y persistir el resultado mediante `Transactional Outbox`, antes de conectar la Saga.

## Ecosistema EventHub

- **Event Service:** creación/publicación de eventos y gestión de capacidad.
- **Booking Service:** creación de reservas y coordinación de la Saga.
- **Payment Service:** ciclo de vida del pago y publicación de su resultado.
