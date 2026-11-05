# 💳 EventHub — Payment Service

**Payment Service** será el microservicio responsable del procesamiento de pagos dentro de **EventHub Reactive**, una plataforma distribuida de gestión de eventos y reservas.

El objetivo es incorporar un tercer *bounded context* y completar el flujo de negocio: **crear evento → solicitar reserva → solicitar pago → confirmar pago → confirmar reserva**. Booking Service actuará como **Saga Orchestrator**; Payment Service gestionará sus propios pagos y publicará el resultado mediante Kafka.

> **Estado:** fase de diseño e inicialización. Las funcionalidades descritas son objetivos de implementación, no capacidades verificadas todavía. El procesamiento del pago se simulará inicialmente, sin pasarela de pagos real.

## Tecnologías previstas

| Área | Tecnología |
|---|---|
| Plataforma | Java 21, Spring Boot 4.1, Maven |
| Arquitectura | DDD, Hexagonal Architecture |
| Aplicación | Spring MVC |
| Persistencia | Spring Data JPA, Hibernate, PostgreSQL, Flyway |
| Mensajería | Apache Kafka, Spring Kafka |
| Fiabilidad | Transactional Outbox, Idempotent Inbox, Payment Idempotency |
| Pruebas | JUnit, Mockito, Testcontainers |
| Infraestructura | Docker Compose |

## Responsabilidades

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
├── pom.xml                 # cuando se inicialice
└── src/                    # cuando se inicialice
```

El código se incorporará progresivamente, empezando por el dominio `Payment` y sus tests.

## Ecosistema EventHub

- **Event Service:** creación/publicación de eventos y gestión de capacidad.
- **Booking Service:** creación de reservas y coordinación de la Saga.
- **Payment Service:** ciclo de vida del pago y publicación de su resultado.
