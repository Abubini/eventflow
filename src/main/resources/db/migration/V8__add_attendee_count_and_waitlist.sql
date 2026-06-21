-- Allow one registration to cover multiple people
ALTER TABLE registrations
    ADD COLUMN attendee_count INT NOT NULL DEFAULT 1 CHECK (attendee_count >= 1);

-- Waitlist table: users who want to be notified when a slot opens
CREATE TABLE waitlist_entries (
                                  id         BIGSERIAL PRIMARY KEY,
                                  user_id    BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                  event_id   BIGINT    NOT NULL REFERENCES events (id) ON DELETE CASCADE,
                                  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                                  notified   BOOLEAN   NOT NULL DEFAULT FALSE,
                                  CONSTRAINT uq_waitlist_user_event UNIQUE (user_id, event_id)
);

CREATE INDEX idx_waitlist_event_id ON waitlist_entries (event_id);
CREATE INDEX idx_waitlist_user_id  ON waitlist_entries (user_id);