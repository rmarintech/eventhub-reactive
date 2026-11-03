package com.rubenmarin.eventhub.event.infrastructure.recovery;

import com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka.KafkaHeadersCodec;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntity;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.EventRecoveryEntityId;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

// Tests de integración: contexto Spring real y PostgreSQL/Kafka en Testcontainers.
@SpringBootTest
@Testcontainers
class DltRecoveryUnblockIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.1.0");

    // SQL directo únicamente para preparar la fecha de caducidad del fixture.
    @Autowired
    private DatabaseClient databaseClient;

    // Repositorio R2DBC real: las pruebas validan persistencia y @Version en PostgreSQL.
    @Autowired
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private DltRecoveryService dltRecoveryService;

    @Autowired
    private KafkaHeadersCodec kafkaHeadersCodec;

    private static final String DLT_TOPIC = "booking-created.DLT";

    @Test
    void shouldRejectOverwrite() {
        // OBJETIVO: un intento antiguo no puede sobrescribir el cambio de otro proceso.
        // Dos lecturas conservan la misma versión; una actualización hace obsoleta la otra.

        // GIVEN: preparar identificador del intento y un mensaje DLT de prueba.
        UUID attemptId = UUID.randomUUID();
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);

        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        Headers originalHeaders = new RecordHeaders()
                .add(
                        "correlationId",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        // La clave compuesta (topic, partition, offset) identifica el registro recuperable.
        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        // Insertar como PENDING; block() suscribe y espera la escritura R2DBC.
        eventRecoveryEntity = dataEventRecoveryRepository.save(eventRecoveryEntity).block();

        Assertions.assertNotNull(eventRecoveryEntity.getVersion());

        // Transición real PENDING -> PROCESSING, persistida con una nueva versión.
        dataEventRecoveryRepository.findById(eventRecoveryEntityId)
                .flatMap(entity -> {
                    entity.markAsProcessing(attemptId);
                    return dataEventRecoveryRepository.save(entity);
                })
                .block();

        // Dos instancias independientes en estado PROCESSING, con la misma versión y el mismo attemptId.
        EventRecoveryEntity instanceA = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        EventRecoveryEntity instanceB = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();

        Assertions.assertNotSame(instanceA, instanceB);
        Assertions.assertEquals(instanceA.getVersion(), instanceB.getVersion());
        Assertions.assertEquals(instanceA.getAttemptId(), instanceB.getAttemptId());
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceA.getStatus());
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceB.getStatus());

        // Proceso A desbloquea el evento y guarda la versión nueva.
        instanceA.markAsPendingFromProcessing();
        dataEventRecoveryRepository.save(instanceA).block();

        // Proceso B opera sobre su snapshot antiguo: pasa la validación en memoria.
        instanceB.markAsRepublished(attemptId);

        // WHEN/THEN: PostgreSQL rechaza la escritura porque la versión ya cambió.
        Assertions.assertThrows(
                OptimisticLockingFailureException.class,
                () -> dataEventRecoveryRepository.save(instanceB).block()
        );

        // El fallo de B no modifica el estado PENDING persistido por A.
        EventRecoveryEntity instanceC = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PENDING, instanceC.getStatus());

    }

    @Test
    void shouldUnblockExpiredRecoveryEvent() {
        // OBJETIVO: si PROCESSING lleva más de cinco minutos, vuelve a PENDING.
        // El test simula el paso del tiempo mediante un UPDATE sobre processing_at.

        // GIVEN: preparar identificador del intento y un mensaje DLT de prueba.
        UUID attemptId = UUID.randomUUID();
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);

        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        Headers originalHeaders = new RecordHeaders()
                .add(
                        "correlationId",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        // La clave compuesta (topic, partition, offset) identifica el registro recuperable.
        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        // Insertar como PENDING; block() suscribe y espera la escritura R2DBC.
        eventRecoveryEntity = dataEventRecoveryRepository.save(eventRecoveryEntity).block();

        Assertions.assertNotNull(eventRecoveryEntity.getVersion());

        // Transición real PENDING -> PROCESSING, persistida con una nueva versión.
        dataEventRecoveryRepository.findById(eventRecoveryEntityId)
                .flatMap(entity -> {
                    entity.markAsProcessing(attemptId);
                    return dataEventRecoveryRepository.save(entity);
                })
                .block();

        EventRecoveryEntity instanceA = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceA.getStatus());


        // Simular que PROCESSING comenzó hace diez minutos sin esperar realmente.
        // El WHERE incluye las tres columnas de la clave compuesta.
        Long updatedRows = databaseClient
                .sql(
                        "UPDATE recovery_events " +
                                "SET processing_at = :date WHERE " +
                                "kafka_topic = :kafkaTopic " +
                                "AND kafka_partition = :kafkaPartition " +
                                "AND kafka_offset= :kafkaOffset")
                .bind("date", Instant.now().minusSeconds(600))
                .bind("kafkaTopic", instanceA.getEventRecoveryEntityId().kafkaTopic())
                .bind("kafkaPartition", instanceA.getEventRecoveryEntityId().kafkaPartition())
                .bind("kafkaOffset", instanceA.getEventRecoveryEntityId().kafkaOffset())
                .fetch()
                .rowsUpdated()
                .block();

        // Garantizar que el fixture modificó exactamente la fila esperada.
        Assertions.assertEquals(1L, updatedRows);

        EventRecoveryEntity instanceB = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceB.getStatus());
        // Precondición: el registro sigue PROCESSING, pero supera los cinco minutos.
        Assertions.assertTrue(instanceB.getProcessingAt().isBefore(Instant.now().minusSeconds(300L)));

        // WHEN: ejecutar el desbloqueo real y esperar a que termine.
        dltRecoveryService.unblockExpired().block();
        // THEN: estado reiniciado y attemptId anterior invalidado.
        EventRecoveryEntity instanceC = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PENDING, instanceC.getStatus());
        Assertions.assertNull(instanceC.getAttemptId());
        Assertions.assertNull(instanceC.getProcessingAt());
        Assertions.assertTrue(instanceC.getPendingAt().isAfter(instanceB.getPendingAt()));
        // La escritura de desbloqueo también incrementa la versión optimista.
        Assertions.assertTrue(instanceC.getVersion() > instanceB.getVersion());

    }

    @Test
    void shouldNotUnblockNonExpiredRecoveryEvent() {
        // OBJETIVO: no tocar eventos PROCESSING cuyo procesamiento aún no ha caducado.
        // GIVEN: preparar identificador del intento y un mensaje DLT de prueba.
        UUID attemptId = UUID.randomUUID();
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = Math.abs(UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L);

        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        Headers originalHeaders = new RecordHeaders()
                .add(
                        "correlationId",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        // La clave compuesta (topic, partition, offset) identifica el registro recuperable.
        EventRecoveryEntityId eventRecoveryEntityId = new EventRecoveryEntityId(kafkaTopic, kafkaPartition, kafkaOffset);
        EventRecoveryEntity eventRecoveryEntity = new EventRecoveryEntity(eventRecoveryEntityId, messageKey, headers, payload);

        // Insertar como PENDING; block() suscribe y espera la escritura R2DBC.
        eventRecoveryEntity = dataEventRecoveryRepository.save(eventRecoveryEntity).block();

        Assertions.assertNotNull(eventRecoveryEntity.getVersion());

        // Transición real PENDING -> PROCESSING, persistida con una nueva versión.
        dataEventRecoveryRepository.findById(eventRecoveryEntityId)
                .flatMap(entity -> {
                    entity.markAsProcessing(attemptId);
                    return dataEventRecoveryRepository.save(entity);
                })
                .block();

        EventRecoveryEntity instanceA = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceA.getStatus());
        // Precondición: el intento comenzó hace menos de cinco minutos.
        Assertions.assertTrue(instanceA.getProcessingAt().isAfter(Instant.now().minusSeconds(300L)));

        // WHEN: ejecutar el desbloqueo; este registro NO debe ser candidato.
        dltRecoveryService.unblockExpired().block();
        // THEN: no cambian estado, timestamps, propietario ni versión.
        EventRecoveryEntity instanceB = dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();
        Assertions.assertEquals(RecoveryStatus.PROCESSING, instanceB.getStatus());
        Assertions.assertEquals(instanceA.getAttemptId(), instanceB.getAttemptId());
        Assertions.assertEquals(instanceA.getPendingAt(), instanceB.getPendingAt());
        Assertions.assertEquals(instanceA.getProcessingAt(), instanceB.getProcessingAt());
        Assertions.assertEquals(instanceA.getVersion(), instanceB.getVersion());
    }

    @Test
    void shouldIgnoreProcessingEventWithNullProcessingAt() {

        // OBJETIVO:
        // Verificar que unblockExpired() ignora un evento en estado PROCESSING
        // cuando processingAt es NULL, ya que representa una inconsistencia de datos.
        //
        // El evento no debe:
        // - Provocar un error que interrumpa el procesamiento.
        // - Volver automáticamente a PENDING.
        // - Perder su attemptId.
        // - Incrementar su versión.
        //
        // Este test utiliza PostgreSQL real mediante Testcontainers
        // y debe ejecutar la implementación real de DltRecoveryService.


        // =====================================================================
        // GIVEN: preparar un evento DLT válido en estado PROCESSING.
        // =====================================================================

        // Identificador único del intento de reprocesamiento.
        UUID attemptId = UUID.randomUUID();

        // Identificación del mensaje original en Kafka.
        // La combinación topic + partition + offset identifica el registro.
        String kafkaTopic = DLT_TOPIC;
        Integer kafkaPartition = 0;
        Long kafkaOffset = Math.abs(
                UUID.randomUUID().getMostSignificantBits() % 1_000_000_000L
        );

        // Clave del mensaje y correlationId para trazabilidad.
        String messageKey = UUID.randomUUID().toString();
        String correlationId = UUID.randomUUID().toString();

        // Construir los headers originales del mensaje Kafka.
        Headers originalHeaders = new RecordHeaders()
                .add(
                        "correlationId",
                        correlationId.getBytes(StandardCharsets.UTF_8)
                );

        // Serializar los headers para almacenarlos en PostgreSQL.
        String headers = kafkaHeadersCodec.serialize(originalHeaders);

        // Payload de prueba que representa un evento de creación de reserva.
        String payload = "{"
                + "\"integrationEventId\":\"" + UUID.randomUUID() + "\","
                + "\"bookingId\":\"" + UUID.randomUUID() + "\","
                + "\"eventId\":\"" + UUID.randomUUID() + "\","
                + "\"userId\":\"" + UUID.randomUUID() + "\""
                + "}";

        // Crear la clave primaria compuesta del registro de recuperación.
        EventRecoveryEntityId eventRecoveryEntityId =
                new EventRecoveryEntityId(
                        kafkaTopic,
                        kafkaPartition,
                        kafkaOffset
                );

        // Crear el registro de recuperación.
        // Su estado inicial es PENDING.
        EventRecoveryEntity eventRecoveryEntity =
                new EventRecoveryEntity(
                        eventRecoveryEntityId,
                        messageKey,
                        headers,
                        payload
                );

        // Persistir el registro inicial.
        // block() suscribe el Mono y espera a que finalice la operación R2DBC.
        eventRecoveryEntity =
                dataEventRecoveryRepository.save(eventRecoveryEntity).block();

        // Verificar que Spring Data R2DBC ha asignado una versión al registro.
        Assertions.assertNotNull(eventRecoveryEntity.getVersion());


        // =====================================================================
        // GIVEN: realizar una transición válida PENDING -> PROCESSING.
        // =====================================================================

        // Recuperar el registro desde PostgreSQL.
        // Aplicar la transición de dominio y persistir el nuevo estado.
        dataEventRecoveryRepository.findById(eventRecoveryEntityId)
                .flatMap(entity -> {

                    // Establece PROCESSING, attemptId y processingAt.
                    entity.markAsProcessing(attemptId);

                    // Persiste la transición e incrementa la versión.
                    return dataEventRecoveryRepository.save(entity);
                })
                .block();

        // Recuperar una copia del estado previo a la inconsistencia.
        EventRecoveryEntity instanceA =
                dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();

        // Confirmar que la transición se ha realizado correctamente.
        Assertions.assertEquals(
                RecoveryStatus.PROCESSING,
                instanceA.getStatus()
        );


        // =====================================================================
        // GIVEN: introducir una inconsistencia directamente en PostgreSQL.
        // =====================================================================

        // Simular un registro corrupto:
        // status = PROCESSING, pero processing_at = NULL.
        //
        // Utilizamos DatabaseClient porque no queremos modificar el modelo
        // de dominio para permitir estados inconsistentes.
        //
        // El WHERE identifica exactamente la fila mediante su PK compuesta.
        Long updatedRows = databaseClient
                .sql(
                        "UPDATE recovery_events " +
                                "SET processing_at = :date WHERE " +
                                "kafka_topic = :kafkaTopic " +
                                "AND kafka_partition = :kafkaPartition " +
                                "AND kafka_offset = :kafkaOffset"
                )

                // Establecer explícitamente NULL utilizando el tipo Instant.
                // DatabaseClient.bind() no admite valores null.
                .bindNull("date", Instant.class)

                // Identificación del registro que queremos corromper.
                .bind(
                        "kafkaTopic",
                        instanceA.getEventRecoveryEntityId().kafkaTopic()
                )
                .bind(
                        "kafkaPartition",
                        instanceA.getEventRecoveryEntityId().kafkaPartition()
                )
                .bind(
                        "kafkaOffset",
                        instanceA.getEventRecoveryEntityId().kafkaOffset()
                )

                // Ejecutar el UPDATE y obtener el número de filas afectadas.
                .fetch()
                .rowsUpdated()
                .block();

        // Garantizar que hemos modificado exactamente el registro esperado.
        Assertions.assertEquals(1L, updatedRows);


        // =====================================================================
        // WHEN: ejecutar la recuperación de eventos PROCESSING caducados.
        // =====================================================================

        // Ejecutar el servicio real, no un mock.
        //
        // unblockExpired() debe:
        // 1. Buscar eventos PROCESSING.
        // 2. Detectar que processingAt es NULL.
        // 3. Registrar la inconsistencia.
        // 4. Excluir el evento del desbloqueo.
        //
        // block() espera a que finalice el flujo reactivo.
        // Si el Mono termina con error, el test fallará.
        dltRecoveryService.unblockExpired().block();


        // =====================================================================
        // THEN: comprobar que el evento inconsistente no se ha modificado.
        // =====================================================================

        // Recuperar nuevamente el registro desde PostgreSQL.
        EventRecoveryEntity instanceB =
                dataEventRecoveryRepository.findById(eventRecoveryEntityId).block();

        // El evento debe permanecer en PROCESSING.
        // No debe volver automáticamente a PENDING.
        Assertions.assertEquals(
                RecoveryStatus.PROCESSING,
                instanceB.getStatus()
        );

        // El identificador del intento debe conservarse.
        Assertions.assertEquals(
                instanceA.getAttemptId(),
                instanceB.getAttemptId()
        );

        // La versión debe permanecer intacta.
        // Esto permite detectar que no se ha realizado ningún UPDATE adicional
        // mediante el repositorio con control de optimistic locking.
        Assertions.assertEquals(
                instanceA.getVersion(),
                instanceB.getVersion()
        );

        // Confirmar que processingAt continúa siendo NULL.
        // El servicio no debe reparar silenciosamente la inconsistencia.
        Assertions.assertNull(instanceB.getProcessingAt());
    }
}
