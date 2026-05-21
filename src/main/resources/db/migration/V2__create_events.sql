CREATE TYPE event_status AS ENUM ('DRAFT', 'PUBLISHED', 'CANCELLED');

CREATE TABLE events (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    location    VARCHAR(300)  NOT NULL,
    date_time   TIMESTAMP     NOT NULL,
    capacity    INTEGER,
    status      event_status  NOT NULL DEFAULT 'DRAFT',
    created_by  BIGINT        NOT NULL REFERENCES users (id),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_events_status    ON events (status);
CREATE INDEX idx_events_date_time ON events (date_time);
CREATE INDEX idx_events_created_by ON events (created_by);
