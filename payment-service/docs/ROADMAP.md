# 🗺️ Payment Service — Roadmap

> **Estado:** planificación inicial. `[ ]` pendiente; `[x]` implementado y comprobado. No marcar tareas como completadas sin evidencia de implementación y pruebas.

## Fase 1 — Inicialización y dominio

- [ ] Inicializar módulo Maven `payment-service` (Java 21, Spring Boot 4.1).
- [ ] Configurar arranque independiente en puerto `8082`.
- [ ] Crear estructura mínima DDD + Hexagonal Architecture, sin paquetes vacíos innecesarios.
- [ ] Definir agregado `Payment` y Value Objects (`PaymentId`, `PaymentRequestId`, `BookingId`, importe y moneda).
- [ ] Definir `PaymentStatus`: `PENDING`, `COMPLETED`, `FAILED`.
- [ ] Definir invariantes y transiciones permitidas.
- [ ] Crear unit tests del dominio.

## Fase 2 — Persistencia

- [ ] Configurar base de datos PostgreSQL propia.
- [ ] Incorporar Flyway y esquema de `payments`.
- [ ] Implementar repository port, entidad JPA y adapter de persistencia.
- [ ] Probar inserción, rehidratación y transiciones con Testcontainers.
- [ ] Definir constraint único para la solicitud lógica `paymentRequestId`.

## Fase 3 — Kafka e idempotencia

- [ ] Definir el contrato versionado de `PaymentRequested`.
- [ ] Implementar consumer y registro transaccional en `inbox_events`.
- [ ] Evitar doble procesamiento del mismo `integrationEventId`.
- [ ] Evitar doble pago para un mismo `paymentRequestId`, incluso con eventos distintos.
- [ ] Persistir el resultado del pago y el evento de salida en un mismo commit local (Transactional Outbox).
- [ ] Implementar relay de Outbox hacia `PaymentCompleted` / `PaymentFailed`.
- [ ] Verificar reentrega y reintentos con PostgreSQL y Kafka Testcontainers.

## Fase 4 — Saga orquestada por Booking Service

- [ ] Adaptar Booking para admitir `PENDING`, `CONFIRMED` y `CANCELLED`.
- [ ] Reservar capacidad y crear Booking `PENDING` con Outbox `PaymentRequested`.
- [ ] Implementar consumer Inbox de resultados de Payment en Booking Service.
- [ ] Confirmar Booking al recibir `PaymentCompleted` de forma idempotente.
- [ ] Cancelar Booking y solicitar liberación de capacidad al recibir `PaymentFailed`.
- [ ] Definir conducta ante mensajes tardíos, contradictorios o fuera de orden.

## Fase 5 — Recuperación y pruebas distribuidas

- [ ] Implementar recuperación de pagos o reservas pendientes agotando un plazo definido.
- [ ] Resolver resultados ambiguos de reserva/liberación HTTP con idempotency keys o reconciliación.
- [ ] Probar caída de PostgreSQL y de Kafka durante procesamiento/publicación.
- [ ] Probar duplicados concurrentes y reintentos tras ACK.
- [ ] Probar compensación fallida y reconciliación posterior.
- [ ] Ejecutar E2E de Saga completa (éxito, pago fallido, timeout y redelivery).
- [ ] Actualizar README y documentación con evidencias reales.

## Principio de ejecución

Trabajaremos por incrementos pequeños, revisando el código y las pruebas de cada paso antes de avanzar. Los detalles del diseño viven en **[ARCHITECTURE.md](ARCHITECTURE.md)**.
