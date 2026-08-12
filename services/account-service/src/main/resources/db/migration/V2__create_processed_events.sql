CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    aggregate_id UUID NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_events_aggregate_id ON processed_events(aggregate_id);
