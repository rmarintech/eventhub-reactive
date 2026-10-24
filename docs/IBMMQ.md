# IBM MQ and Spring JMS — EventHub

> **Verification scope (20 October 2026).** Booking Service now creates separate Kafka and IBM MQ Outbox records in the same local PostgreSQL transaction. Both relay paths were exercised locally. IBM MQ consumption and Inbox persistence were also tested with isolated IBM MQ and PostgreSQL Testcontainers. This is **not** an exactly-once or distributed transaction guarantee.

## Purpose and architecture

```text
Booking created -> TransactionalBookingUseCase (@Transactional)
    |-- INSERT bookings
    |-- INSERT outbox_events [broker_name = KAFKA, status = PENDING]
    +-- INSERT outbox_events [broker_name = IBMMQ, status = PENDING]
                  | one PostgreSQL transaction
                  v
    OutboxKafkaScheduler              OutboxIBMMQScheduler
        |                                 |
        v                                 v
    KafkaIntegrationEventAdapter     IbmMqIntegrationEventAdapter
        |                                 | JmsTemplate.convertAndSend(JSON)
        v                                 v
    topic booking-created             BOOKING.CREATED.Q / QM1
        |                                 |
        v                                 v
    Event Service Kafka consumer      BookingCreatedMqConsumer (@JmsListener)
        |                                 |
        v                                 v
    Event Service R2DBC Inbox         Booking Service JPA Inbox
```

- Booking Service uses Spring MVC / JPA, `spring-boot-starter-jms`, and `com.ibm.mq:mq-jms-spring-boot-starter:4.0.1`.
- Flyway V5 introduces `broker_name` (`KAFKA` / `IBMMQ`) to route individual Outbox records to the appropriate relay. The event payload contains `integrationEventId`, `bookingId`, `eventId`, and `userId`.
- Separate schedulers select `PENDING` records for their broker. `OutboxIBMMQScheduler` uses `IbmMqIntegrationEventAdapter`, which serializes the integration event to JSON and publishes synchronously via `JmsTemplate`. Successful delivery marks only that broker's Outbox record `PUBLISHED`.
- `BookingCreatedMqConsumer` deserializes the JSON and invokes `ProcessBookingCreatedUseCase`. Booking Service's PostgreSQL `inbox_events(integration_event_id UUID PRIMARY KEY, processed_at TIMESTAMPTZ)` prevents duplicate Inbox rows for the same logical `integrationEventId` (Flyway V6 column rename).
- Local atomicity covers Booking plus the **two Outbox inserts**, not either external broker. Each relay can fail independently; publication remains **at least once** and duplicates are possible. Do not claim atomic dual-broker delivery or exactly-once business effects.

## Local IBM MQ setup

At the project root, `compose.ibmmq.yaml` defines the MQ container (`eventhub-ibmmq`), queue manager `QM1`, client port `1414` and console port `9443`, with a persistent `ibmmq-data` volume and mounted `./ibmmq/init.mqsc`.

Example **local development only** connection settings in `booking-service/src/main/resources/application.yaml`:

```yaml
ibm:
  mq:
    queueManager: QM1
    channel: DEV.APP.SVRCONN
    connName: localhost(1414)
    user: app
    password: ${IBM_MQ_PASSWORD}
```

`ibm.mq` must be **at YAML root**, not nested inside `spring`. Configure `IBM_MQ_PASSWORD` locally; do not commit real credentials. The previously used sample container password was development-only.

```powershell
docker compose -f compose.ibmmq.yaml up -d
Get-Content .\ibmmq\init.mqsc | docker exec -i eventhub-ibmmq runmqsc QM1
```

The MQSC setup needed for the tested scenario is:

```text
* Create the source and backout queues on first setup
DEFINE QLOCAL('BOOKING.CREATED.Q') REPLACE
DEFINE QLOCAL('BOOKING.BACKOUT.Q') REPLACE

* Route messages that exceed the backout threshold
ALTER QLOCAL('BOOKING.CREATED.Q') BOTHRESH(3) BOQNAME('BOOKING.BACKOUT.Q')

* Application permissions for production, consumption and inspection
SET AUTHREC PROFILE('BOOKING.CREATED.Q') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(PUT,GET,INQ,BROWSE)
SET AUTHREC PROFILE('BOOKING.BACKOUT.Q') OBJTYPE(QUEUE) PRINCIPAL('app') AUTHADD(PUT,INQ,PASSALL,BROWSE)
```

**Note:** `DEFINE ... REPLACE` is suitable for initializing an empty local environment, **not for routinely reapplying to existing queues with messages**. For an existing queue, use `ALTER` and `SET AUTHREC` without redefining it. MQSC uses `*` at the start of the line for comments.

## Manual tests and evidence

| Test | Observed result |
| --- | --- |
| JMS producer publishes booking JSON | `BOOKING.CREATED.Q` showed `CURDEPTH(1)` |
| `@JmsListener` consumes | Logs showed a `BookingCreatedIntegrationEvent` JSON payload; source depth became `CURDEPTH(0)` |
| Listener deliberately throws `RuntimeException` | Same message was repeatedly redelivered following transaction rollback |
| Source configured with `BOTHRESH(3)` and `BOQNAME(BOOKING.BACKOUT.Q)` | After required queue permissions, failing message appeared in `BOOKING.BACKOUT.Q` (`CURDEPTH(1)`) |
| Backout inspector calls `JmsTemplate.browse()` | Logs showed `JMSMessageID` and original JSON payload; backout depth remained `CURDEPTH(1)` |

Commands (PowerShell, from the project root or any directory with Docker available):

```powershell
"DISPLAY QLOCAL('BOOKING.CREATED.Q') CURDEPTH BOTHRESH BOQNAME" | docker exec -i eventhub-ibmmq runmqsc QM1
"DISPLAY QLOCAL('BOOKING.BACKOUT.Q') CURDEPTH" | docker exec -i eventhub-ibmmq runmqsc QM1

docker exec eventhub-ibmmq dspmqaut -m QM1 -t q -n BOOKING.CREATED.Q -p app
docker exec eventhub-ibmmq dspmqaut -m QM1 -t q -n BOOKING.BACKOUT.Q -p app

docker exec eventhub-ibmmq sh -c "tail -n 60 /var/mqm/qmgrs/QM1/errors/AMQERR01.LOG"
```

## Redelivery and backout behavior

- The tested Spring Boot JMS listener used auto-configured transactional handling. A listener exception caused rollback, so IBM MQ redelivered the message.
- `BOTHRESH(3)` supplies the queue's backout threshold and `BOQNAME` identifies the target backout queue. **The IBM MQ JMS client**, rather than the queue manager automatically moving messages on its own, performed the backout handling in this test.
- `BOOKING.BACKOUT.Q` is the **backout queue** for repeatedly failing consumer messages; it is distinct from the queue manager's **dead-letter queue** (e.g. `DEV.DEAD.LETTER.QUEUE`).
- IBM MQ errors revealed required permissions incrementally: `GET`/`BROWSE` for source consumption, and `PASSALL` on the backout queue for the JMS client to preserve message context during requeue. The MQ error log specifically identified missing `browse` and `passall` before those permissions were granted.
- This threshold does **not** establish a general guarantee of exactly three total deliveries in every JMS configuration. The observed successful handling was with the particular local IBM MQ JMS/Spring configuration.

## Non-destructive backout inspection

`BookingBackoutInspector` calls `JmsTemplate.browse("BOOKING.BACKOUT.Q", callback)` and uses `QueueBrowser.getEnumeration()` to iterate JMS messages. For `TextMessage`, it logs `getJMSMessageID()` and `getText()`. Browsing does **not acknowledge or remove** the messages; their persistence was checked with `CURDEPTH(1)` after inspection.

## Automated integration tests (20 October 2026)

`IbmMqContainerIT` uses **IBM MQ Testcontainers** (`com.ibm.mq:mq-java-testcontainer:2.0.5`) and a separate **PostgreSQL 17 Testcontainer**, with Spring configuration overridden through `@DynamicPropertySource`. Flyway initializes the isolated Booking Service database.

Green tests reported for:

1. Starting both isolated containers and checking the two MQ queues.
2. Publishing and receiving a real JMS text message through `JmsTemplate`.
3. Publishing `BookingCreatedIntegrationEvent` and verifying that the real `BookingCreatedMqConsumer` records the booking ID in PostgreSQL Inbox.
4. Publishing the same integration event twice, observing one Inbox row and empty source/backout queues afterward.

**Test scope caveat:** checking `CURDEPTH(0)` alone cannot prove the exact moment at which the second JMS delivery was transactionally acknowledged. These green tests demonstrate observable deduplication and queue draining, not exactly-once processing under every failure window. They also do not constitute a broker-backed automated Kafka test or a complete test of the dual-Outbox relay itself.

**Testcontainers authentication pitfall:** `MQContainer.configure()` writes its own default app password (`app`) to `/run/secrets/mqAppPassword`. Copying a custom file to the same path is overwritten. Use `.withAppPassword("passw0rd")` in the local integration-test container; this is **test-only** and is not a deployment credential.

## Scope, safety and pending work

- Manual backout, rollback/redelivery, browsing, and controlled reprocessing checks complement the automated happy-path and duplicate-event tests. Do not claim full automated failure-path coverage.
- Restore the normal consumer after deliberate failure injection. Queue permissions and credentials must be reviewed before production use.
- Kafka DLT/offset behavior and robust multi-instance relay coordination remain outside the scope of these IBM MQ tests.
- Kafka DLT publishing and error-handler unit tests have subsequently been added; remaining work includes Kafka broker-backed failure/offset tests and controlled DLT replay.

See also [KAFKA.md](KAFKA.md), [MICROSERVICES_COMMUNICATION.md](MICROSERVICES_COMMUNICATION.md) and [ROADMAP.md](ROADMAP.md).
