package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery;

import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

//@SpringBootTest
@DataR2dbcTest
@Testcontainers
public class SpringDataEventRecoveryRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private SpringDataEventRecoveryRepository springDataEventRecoveryRepository;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldSaveAndRetrieveRecoveryEvent() {

        EventRecoveryEntityId id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE
        );
        EventRecoveryEntity newEntity = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        Mono<EventRecoveryEntity> saved = springDataEventRecoveryRepository.save(newEntity);

        Mono<EventRecoveryEntity> found = saved.flatMap(
                eventRecoveryEntity -> {
                    return springDataEventRecoveryRepository.findById(eventRecoveryEntity.getEventRecoveryEntityId());
                });

        StepVerifier.create(found)
                .assertNext(entity -> {
                    Assertions.assertEquals(id, entity.getEventRecoveryEntityId());
                    Assertions.assertEquals(RecoveryStatus.PENDING, entity.getStatus());
                    Assertions.assertNotNull(entity.getPendingAt());
                    Assertions.assertNotNull(entity.getVersion());
                })
                .verifyComplete();
    }

    @Test
    void shouldPreventDuplicateRecoveryEvents() {

        EventRecoveryEntityId id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE
        );
        EventRecoveryEntity newEntity1 = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        EventRecoveryEntity newEntity2 = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        Mono<EventRecoveryEntity> saved = springDataEventRecoveryRepository.save(newEntity1)
                .flatMap(e -> {
                    return springDataEventRecoveryRepository.save(newEntity2);
                });

        StepVerifier.create(saved)
                .expectError(DuplicateKeyException.class)
                .verify();
    }

    @Test
    void shouldUpdateRecoveryStatus() {

        EventRecoveryEntityId id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE
        );

        EventRecoveryEntity newEntity = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        UUID attemptId = UUID.randomUUID();

        Mono<EventRecoveryEntity> result =
                springDataEventRecoveryRepository.save(newEntity)
                        .flatMap(saved -> {
                            saved.markAsProcessing(attemptId);
                            return springDataEventRecoveryRepository.save(saved);
                        })
                        .flatMap(updated ->
                                springDataEventRecoveryRepository.findById(id)
                        );

        StepVerifier.create(result)
                .assertNext(entity -> {
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, entity.getStatus());
                    Assertions.assertEquals(attemptId, entity.getAttemptId());
                    Assertions.assertNotNull(entity.getProcessingAt());
                    Assertions.assertNotNull(entity.getPendingAt());
                })
                .verifyComplete();
    }

    @Test
    void shouldIncrementVersionOnUpdate() {

        EventRecoveryEntityId id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE
        );

        EventRecoveryEntity newEntity = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        Mono<EventRecoveryEntity> result =
                springDataEventRecoveryRepository.save(newEntity)
                        .flatMap(saved -> {

                            Long initialVersion = saved.getVersion();

                            Assertions.assertNotNull(initialVersion);

                            saved.markAsProcessing(UUID.randomUUID());

                            return springDataEventRecoveryRepository.save(saved)
                                    .flatMap(updated ->
                                            springDataEventRecoveryRepository.findById(id)
                                                    .doOnNext(found ->
                                                            Assertions.assertEquals(
                                                                    initialVersion + 1,
                                                                    found.getVersion()
                                                            )
                                                    )
                                    );
                        });

        StepVerifier.create(result)
                .assertNext(entity -> {
                    Assertions.assertEquals(RecoveryStatus.PROCESSING, entity.getStatus());
                    Assertions.assertNotNull(entity.getVersion());
                })
                .verifyComplete();
    }

    @Test
    void shouldRejectConcurrentModification() {
        EventRecoveryEntityId id = new EventRecoveryEntityId(
                "booking-created.DLT",
                0,
                25L
        );
        EventRecoveryEntity newEntity = new EventRecoveryEntity(
                id,
                "booking-123",
                "{}",
                "{\"integrationEventId\":\"550e8400-e29b-41d4-a716-446655440000\"}"
        );

        Mono<EventRecoveryEntity> saved = springDataEventRecoveryRepository.save(newEntity);

        Mono<EventRecoveryEntity> result = saved
                .flatMap(entity ->
                        Mono.zip(
                                springDataEventRecoveryRepository.findById(id),
                                springDataEventRecoveryRepository.findById(id)
                        )
                )
                .flatMap(tuple -> {

                    EventRecoveryEntity entityA = tuple.getT1();
                    EventRecoveryEntity entityB = tuple.getT2();

                    entityA.markAsProcessing(UUID.randomUUID());
                    entityB.markAsProcessing(UUID.randomUUID());

                    return springDataEventRecoveryRepository.save(entityA)
                            .then(springDataEventRecoveryRepository.save(entityB));
                });


        StepVerifier.create(result)
                .expectError(OptimisticLockingFailureException.class)
                .verify();
    }
}
