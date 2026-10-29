package com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery;

import com.rubenmarin.eventhub.event.infrastructure.recovery.RecoveryStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Table("recovery_events")
public class EventRecoveryEntity {
    private static final Logger log = LoggerFactory.getLogger(EventRecoveryEntity.class);
    @Id
    @Embedded.Nullable
    private EventRecoveryEntityId eventRecoveryEntityId;
    private Instant pendingAt;
    private Instant processingAt;
    private Instant republishedAt;
    private UUID attemptId;
    private String messageKey;
    private String headers;
    private String payload;
    private RecoveryStatus status;

    //Optimistic locking
    @Version
    private Long version;

    public EventRecoveryEntity(EventRecoveryEntityId eventRecoveryEntityId, String messageKey,String headers,String payload) {
        this.eventRecoveryEntityId = eventRecoveryEntityId;
        this.messageKey = messageKey;
        this.headers = headers;
        this.payload = payload;
        this.status = RecoveryStatus.PENDING;
        this.pendingAt = Instant.now();
    }

    public void markAsPendingFromProcessing() {
        if (this.status != RecoveryStatus.PROCESSING) {
            throw new IllegalStateException("Recovery event is not processing");
        }

        this.status = RecoveryStatus.PENDING;
        this.processingAt = null;
        this.pendingAt = Instant.now();
        this.attemptId = null;
    }

    public void markAsProcessing(UUID attemptId) {
        if (this.status != RecoveryStatus.PENDING) {
            throw new IllegalStateException("Recovery event is not pending");
        }
        if (Objects.isNull(attemptId)) {
            throw new IllegalStateException("AttemptId cannot be null");
        }

        this.status = RecoveryStatus.PROCESSING;
        this.processingAt = Instant.now();
        this.attemptId = attemptId;
    }

    public void markAsRepublished(UUID attemptId) {

        if (this.status != RecoveryStatus.PROCESSING) {
            throw new IllegalStateException("Recovery event is not processing");
        }
        if (!Objects.equals(this.attemptId,attemptId)) {
            throw new IllegalStateException("Event Attempt mismatch. Expected: " + this.attemptId);
        }

        this.status = RecoveryStatus.REPUBLISHED;
        this.republishedAt = Instant.now();

    }

    public EventRecoveryEntityId getEventRecoveryEntityId() {
        return eventRecoveryEntityId;
    }

    public Instant getPendingAt() {
        return pendingAt;
    }

    public Instant getProcessingAt() {
        return processingAt;
    }

    public Instant getRepublishedAt() {
        return republishedAt;
    }

    public UUID getAttemptId() {
        return attemptId;
    }

    public String getPayload() {
        return payload;
    }

    public RecoveryStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getHeaders() {
        return headers;
    }
}


