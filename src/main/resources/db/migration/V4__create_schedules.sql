CREATE TABLE schedules (
    id            BIGSERIAL PRIMARY KEY,
    event_id      BIGINT       NOT NULL REFERENCES events (id) ON DELETE CASCADE,
    session_title VARCHAR(200) NOT NULL,
    description   VARCHAR(1000),
    start_time    TIMESTAMP    NOT NULL,
    end_time      TIMESTAMP    NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_schedules_event_id ON schedules (event_id);
