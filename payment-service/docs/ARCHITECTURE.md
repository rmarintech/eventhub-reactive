# 🏗️ Payment Service — Arquitectura y diseño técnico

> **Documento de diseño, no descripción de una implementación existente.** La primera versión simulará el procesamiento del pago; no se almacenarán credenciales ni datos de tarjetas.

## 1. Contextos y responsabilidades

| Servicio | Responsabilidad | Persistencia |
|---|---|---|
| Event Service | Publicar eventos, reservar y liberar plazas | PostgreSQL propia, WebFlux/R2DBC |
| Booking Service | Gestionar reservas y **orquestar la Saga** | PostgreSQL propia, Spring MVC/JPA |
| Payment Service | Gestionar y resolver solicitudes de pago | PostgreSQL propia, Spring MVC/JPA |

Cada servicio es dueño de sus datos. Payment Service conoce referencias (`bookingId`, `paymentRequestId`), **no** el agregado de dominio `Booking` ni su repositorio.

## 2. Visión general del flujo

Crear/publicar un evento es una operación preparatoria independiente. La Saga empieza con la **solicitud de reserva**.

```text
Cliente
  │ 1. Crear/publicar Event
  ▼
Event Service
  ▲
  │ 2. Solicitar Booking
  ▼
Booking Service ── REST: reserve(capacity) ──► Event Service
  │
  ├─ transacción local: Booking(PENDING) + Outbox(PaymentRequested)
  │
  ▼
Kafka: PaymentRequested
  │
  ▼
Payment Service
  ├─ transacción local: Inbox + Payment + Outbox(resultado)
  │
  ├─────────────────────┬────────────────────┐
  ▼                     ▼                    │
Kafka: PaymentCompleted  Kafka: PaymentFailed
  │                     │
  └──────────┬──────────┘
             ▼
Booking Service (Inbox de resultados)
  ├─ COMPLETED → Booking(CONFIRMED)
  └─ FAILED → Booking(CANCELLED) + compensación REST: release(capacity)
```

**Orden del flujo:** separar *plaza reservada* de *reserva confirmada*. No se debe confirmar una reserva antes del resultado satisfactorio del pago.

## 3. Modelo de dominio Payment

**Aggregate Root:** `Payment`.

Atributos propuestos:

- `PaymentId`: identidad del pago.
- `PaymentRequestId`: identidad estable de la operación de pago (idempotencia de negocio).
- `BookingId`: reserva que originó la solicitud.
- `Amount` + `Currency`: valor monetario; importe con `BigDecimal`.
- `PaymentStatus`: `PENDING`, `COMPLETED`, `FAILED`.
- Fechas de creación y resolución, según lo requieran los casos de uso.

Transiciones propuestas:

```text
                 create()
                    │
                    ▼
                 PENDING
                 /     \
         complete()   fail()
              ▼         ▼
         COMPLETED   FAILED
```

Los estados terminales no deberían cambiar mediante las operaciones normales. Las reglas concretas (importe cero, reintentos de pago, fallos técnicos frente a rechazo de negocio) se decidirán y cubrirán con tests antes de implementarse.

## 4. Saga Orchestration

Booking Service coordina la Saga. Un ejemplo de flujo correcto:

1. Reserva una plaza en Event Service mediante una llamada REST.
2. Persiste `Booking(PENDING)` y un Outbox `PaymentRequested` en **un mismo commit**.
3. El relay publica `PaymentRequested` en Kafka.
4. Payment Service recibe la solicitud y, en una transacción local, registra Inbox, Payment y Outbox con el resultado.
5. El relay de Payment Service publica `PaymentCompleted` o `PaymentFailed`.
6. Booking Service registra el resultado en su Inbox y actualiza el estado de Booking.
7. En caso de fallo del pago, Booking Service ejecuta o programa la compensación para liberar la plaza en Event Service.

**Límite importante:** la llamada REST para reservar la plaza y el commit local de Booking **no forman una transacción atómica**. Si la respuesta se pierde o falla el commit, pueden existir resultados ambiguos. La compensación síncrona es *best effort*; necesitaremos idempotencia/reconciliación para una recuperación duradera.

## 5. Contratos de integración propuestos

`PaymentRequested` debe contener, como mínimo:

```text
integrationEventId     # identificador único de este mensaje
paymentRequestId       # identificador estable de la operación lógica
bookingId              # correlación con Booking Service
amount / currency      # datos necesarios para procesar el pago
```

`PaymentCompleted` y `PaymentFailed` deben incluir identificadores suficientes (`integrationEventId`, `paymentRequestId`, `bookingId`, `paymentId`) para correlacionar y deduplicar. Un fallo de negocio puede necesitar un código de motivo no sensible. Se definirán versiones de contrato y claves Kafka antes de conectar los servicios.

## 6. Idempotent Inbox vs Payment Idempotency

**Inbox:** evita volver a ejecutar el procesamiento de un `integrationEventId` ya registrado. Requiere restricción única en PostgreSQL y manejo correcto de carreras entre consumidores.

**Payment Idempotency:** evita procesar dos veces la **misma solicitud lógica**, aunque lleguen dos eventos con diferentes `integrationEventId`.

```text
PaymentRequested(eventId=A, paymentRequestId=123)
PaymentRequested(eventId=B, paymentRequestId=123)
                  │
Inbox: A y B son mensajes distintos
Payment: 123 representa una única operación
```

El registro Inbox, la persistencia del pago y el Outbox del resultado deben pertenecer a **una transacción local**. Un simple `existsById()` previo no basta como protección concurrente; se necesitarán restricciones únicas y tratamiento de conflictos.

También Booking Service necesitará Inbox para recibir idempotentemente `PaymentCompleted` y `PaymentFailed`.

## 7. Transactional Outbox y entrega

```text
BEGIN
  INSERT inbox_events     (si mensaje nuevo)
  INSERT/UPDATE payments  (si operación lógica nueva)
  INSERT outbox_events    (resultado para Booking)
COMMIT

Outbox relay → Kafka → Booking Inbox
```

El Outbox evita perder el **registro de intención de publicación**, pero el envío al broker y la actualización del estado Outbox no son atómicos. Tras un ACK seguido de un fallo de base de datos, el relay podría reenviar; los consumidores deben ser idempotentes.

La garantía práctica buscada es **at-least-once**, no exactly-once a través de las bases de datos y Kafka.

## 8. Compensaciones y límites

- `PaymentCompleted`: confirmar Booking solo si la reserva sigue en un estado compatible.
- `PaymentFailed`: cancelar Booking y liberar capacidad; no dar por realizada la compensación hasta confirmarla o persistir una tarea recuperable.
- **Timeout:** distinguir resultado desconocido de pago rechazado. No cancelar a ciegas un pago que podría haberse completado.
- **Mensajes tardíos o contradictorios:** establecer reglas explícitas de transición y reconciliación.
- **Reserva de capacidad REST:** necesita identidad idempotente por operación para resistir reintentos y respuestas perdidas.
- **Múltiples instancias:** no depender únicamente de locks en memoria para coordinar trabajo distribuido.

## 9. Hexagonal Architecture prevista

```text
infrastructure/adapter/in/kafka   → PaymentRequestedConsumer
             │
             ▼
application/port/in               → ProcessPaymentRequestUseCase
             │
             ▼
application/service               → coordinación transaccional
             │
             ▼
domain/model                      → Payment y Value Objects
             │
             ▼
application/port/out              → PaymentRepository, Inbox, Outbox
             ▲
             │
infrastructure/adapter/out         → JPA/PostgreSQL y Kafka relay
```

El dominio no dependerá de Spring, JPA o Kafka. Los contratos y adapters se introducirán de forma incremental, cuando los use cases los necesiten.

## 10. Estrategia de pruebas

- **Unit tests:** invariantes de Payment, transiciones válidas/inválidas y reglas de idempotencia.
- **Integration tests:** transacción Inbox/Payment/Outbox; constraints únicos; carreras concurrentes; persistencia JPA/PostgreSQL Testcontainers.
- **Kafka integration:** redelivery, eventos duplicados, fallos del relay y reintentos con Kafka Testcontainers.
- **E2E Saga:** pago correcto → reserva confirmada; pago fallido → reserva cancelada + plaza liberada; caída temporal de dependencias y reconciliación.

**Estado de pruebas:** pendientes de desarrollo para Payment Service.

## 11. Relación con el trabajo existente

EventHub ya cuenta con patrones Outbox/Inbox, Kafka DLT recovery y mecanismos de resiliencia en los servicios actuales. Payment Service **reutilizará conceptos y decisiones**, pero no se considerará implementado por el hecho de existir ese código en otro servicio.

Consultar el [roadmap](ROADMAP.md) para el progreso de implementación.
