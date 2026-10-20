CREATE TABLE IF NOT EXISTS inbox_events (
    event_id UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);