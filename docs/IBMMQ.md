# IBM MQ and Spring JMS — EventHub

> **Manually verified.** This documents a separate learning integration in the independent imperative `booking-service`, alongside the existing Kafka/Transactional Outbox integration. It does **not** establish that the same Outbox event is reliably published to both brokers.

## Purpose and architecture

```text
BookingCreatedIntegrationEvent(bookingId, eventId, userId)
    -> IbmMqBookingPublisher (infrastructure output adapter)
    -> JmsTemplate.convertAndSend("BOOKING.CREATED.Q", JSON String)
    -> IBM MQ / QM1
    -> BookingCreatedMqConsumer (infrastructure input adapter)
    -> @JmsListener(destination = "BOOKING.CREATED.Q")
```

- `booking-service` uses **Spring MVC / JPA** and `spring-boot-starter-jms` together with `com.ibm.mq:mq-jms-spring-boot-starter:4.0.1`.
- `IbmMqBookingPublisher` converts the integration-event record into a **JSON String** before calling `JmsTemplate.convertAndSend()`. Passing a Jackson `JsonNode` directly is not the tested JMS text-message approach.
- `BookingCreatedMqConsumer` receives the text payload with `@JmsListener`.
- Direct Spring JMS publishing is an isolated learning path. Kafka publication is still driven by the Transactional Outbox. The two paths do not currently constitute coordinated, atomic dual publication.

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

## Scope, safety and pending work

- Manual local verification only; no automated IBM MQ broker-backed test suite has been evidenced.
- Restore the normal consumer after the deliberate `RuntimeException` redelivery exercise; leaving a permanently failing listener running will continually retry/backout new messages.
- No business-side processing or Inbox/idempotency scheme has been implemented for this JMS consumer. Backout and retry do not prevent duplicate business effects by themselves.
- No automated safe replay/reprocessing workflow from `BOOKING.BACKOUT.Q` to `BOOKING.CREATED.Q` has been implemented. **This is the next learning step.**
- Queue permissions and local credentials should be reviewed and minimized before any production deployment; examples are intended for a local test container.

See also [KAFKA.md](KAFKA.md), [MICROSERVICES_COMMUNICATION.md](MICROSERVICES_COMMUNICATION.md) and [ROADMAP.md](ROADMAP.md).
