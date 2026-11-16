-- Audit entries are append-only in the application's business layer.
ALTER TABLE audit_events ADD COLUMN entity_type VARCHAR(64);
ALTER TABLE audit_events ADD COLUMN entity_id BIGINT;
ALTER TABLE audit_events ADD COLUMN action VARCHAR(40);
ALTER TABLE audit_events ADD COLUMN before_json TEXT;
ALTER TABLE audit_events ADD COLUMN after_json TEXT;
CREATE INDEX idx_audit_entity ON audit_events(entity_type,entity_id,created_at DESC);
