-- Add ticket_code (UUID) and scanned flag to registrations
ALTER TABLE registrations
    ADD COLUMN ticket_code UUID        NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN scanned     BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN scanned_at  TIMESTAMP   NULL;

-- Each ticket code must be unique
ALTER TABLE registrations
    ADD CONSTRAINT uq_ticket_code UNIQUE (ticket_code);

CREATE INDEX idx_registrations_ticket_code ON registrations (ticket_code);