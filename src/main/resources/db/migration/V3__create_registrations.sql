CREATE TABLE registrations (
                               id            BIGSERIAL   PRIMARY KEY,
                               user_id       BIGINT      NOT NULL REFERENCES users (id),
                               event_id      BIGINT      NOT NULL REFERENCES events (id),
                               registered_at TIMESTAMP   NOT NULL DEFAULT NOW(),
                               status        VARCHAR(50) NOT NULL DEFAULT 'CONFIRMED',
                               CONSTRAINT uq_user_event UNIQUE (user_id, event_id)
);

CREATE INDEX idx_reg_user_id  ON registrations (user_id);
CREATE INDEX idx_reg_event_id ON registrations (event_id);