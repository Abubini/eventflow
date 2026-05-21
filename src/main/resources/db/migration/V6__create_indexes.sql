-- Additional composite and search indexes
CREATE INDEX idx_events_status_date ON events (status, date_time);
CREATE INDEX idx_events_location ON events (location);
CREATE INDEX idx_registrations_status ON registrations (status);
