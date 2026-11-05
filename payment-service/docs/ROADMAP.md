# 🗺️ Payment Service — Roadmap

> **Estado:** dominio, persistencia y pruebas de idempotencia secuencial implementados; mensajería y Saga pendientes. `[ ]` pendiente; `[x]` implementado y comprobado. No marcar tareas como completadas sin evidencia de implementación y pruebas.

## Fase 1 — Inicialización y dominio

- [x] Inicializar módulo Maven `payment-service` (Java 21, Spring Boot 4.1).
- [x] Configurar arranque independiente en puerto `8082`.
- [x] Crear estructura mínima DDD + Hexagonal Architecture, sin paquetes vacíos innecesarios.
- [x] Definir agregado `Payment` y Value Objects (`PaymentId`, `PaymentRequestId`, `BookingId`, importe y moneda).
- [x] Definir `PaymentStatus`: `PENDING`, `COMPLETED`, `FAILED`.
- [x] Definir invariantes y transiciones permitidas.
- [x] Crear unit tests del dominio.

## Fase 2 — Persistencia

- [x] Configurar base de datos PostgreSQL propia.
- [x] Incorporar Flyway y esquema de `payments`.
- [x] Implementar repository port, entidad JPA y adapter de persistencia.
- [x] Probar inserción, rehidratación y transiciones con Testcontainers.
- [x] Definir constraint único para la solicitud lógica `paymentRequestId`.

## Fase 3 — Kafka e idempotencia

**Avance actual:** `CreatePaymentService` implementa idempotencia secuencial por `paymentRequestId`, con pruebas unitarias y de integración. La restricción `UNIQUE` protege contra duplicados concurrentes, pero el segundo intento puede devolver un error; el test del resultado idempotente esperado permanece `@Disabled`.

- [ ] Definir el contrato versionado de `PaymentRequested`.
- [ ] Implementar consumer y registro transaccional en `inbox_events`.
- [ ] Evitar doble procesamiento del mismo `integrationEventId`.
- [ ] Evitar doble pago para un mismo `paymentRequestId`, incluso con eventos distintos (regla de persistencia y de negocio implementada; pendiente validarla dentro del procesamiento Kafka y resolver colisiones concurrentes).
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
- [x] Actualizar README y documentación con el estado verificado hasta la fase de idempotencia secuencial.
- [ ] Actualizar documentación con evidencias de Kafka, Inbox/Outbox y Saga cuando se implementen.

## Principio de ejecución

Trabajaremos por incrementos pequeños, revisando el código y las pruebas de cada paso antes de avanzar. Los detalles del diseño viven en **[ARCHITECTURE.md](ARCHITECTURE.md)**.
