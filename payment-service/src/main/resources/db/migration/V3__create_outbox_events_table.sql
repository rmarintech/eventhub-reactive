CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    message_key VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,

    CONSTRAINT chk_outbox_status
       CHECK (status IN ('PENDING', 'PUBLISHED')),

    CONSTRAINT chk_outbox_published_at
       CHECK (
           (status = 'PENDING' AND published_at IS NULL)
               OR
           (status = 'PUBLISHED' AND published_at IS NOT NULL)
           )
);

CREATE INDEX idx_outbox_status_created_at
    ON outbox_events (status, created_at);