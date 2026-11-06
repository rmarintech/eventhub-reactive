CREATE TABLE inbox_events (
    integration_event_id UUID PRIMARY KEY,
    received_at TIMESTAMPTZ NOT NULL
);