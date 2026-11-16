-- Outbound reminder consent defaults OFF; no students are subscribed automatically.
ALTER TABLE students ADD COLUMN reminder_consent BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE reminder_dispatches (
  id BIGSERIAL PRIMARY KEY,
  payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
  kind VARCHAR(30) NOT NULL,
  phone VARCHAR(255) NOT NULL,
  message TEXT NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL UNIQUE,
  status VARCHAR(20) NOT NULL,
  attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
  last_attempt_at TIMESTAMP,
  sent_at TIMESTAMP,
  last_error VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_reminder_payment_kind UNIQUE(payment_id,kind)
);
CREATE INDEX idx_reminder_status_attempts ON reminder_dispatches(status,attempts,id);
