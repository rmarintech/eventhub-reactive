package com.rubenmarin.paymentservice.infrastructure.adapter.out.persistence.jpa.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

public interface SpringDataInboxRepository extends JpaRepository<InboxJpaEntity, UUID> {

    /**
     *
     * @param integrationEventId
     * @param receivedAt
     * @return
     *
     * @Query: define la consulta.
     * nativeQuery = true: indica que utilizamos SQL nativo, no JPQL.
     * @Modifying: indica que la consulta modifica datos.
     * @Param: vincula los parámetros con la consulta.
     * int: devuelve el número de filas insertadas.
     */
    @Modifying
    @Transactional // Los métodos personalizados de Spring Data JPA que ejecutan consultas modificadoras no reciben automáticamente una configuración transaccional.
    @Query(value = """
            INSERT INTO inbox_events (integration_event_id, received_at)
            VALUES (:integrationEventId, :receivedAt)
            ON CONFLICT (integration_event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfNotExists(
            @Param("integrationEventId") UUID integrationEventId,
            @Param("receivedAt") Instant receivedAt
    );
}
