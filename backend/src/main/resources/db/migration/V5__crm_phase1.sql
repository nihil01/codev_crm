-- CRM Phase 1: lead pipeline, request audit, and student details.
CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    source VARCHAR(30) NOT NULL,
    stage VARCHAR(30) NOT NULL,
    assigned_manager_id BIGINT REFERENCES users(id),
    consultation_at TIMESTAMP,
    lost_reason TEXT,
    interested_course VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_lost_reason CHECK (stage <> 'ITIRILDI' OR length(trim(coalesce(lost_reason, ''))) > 0),
    CONSTRAINT ck_consultation_date CHECK (stage <> 'SINAQ_KONSULTASIYA' OR consultation_at IS NOT NULL)
);
CREATE INDEX idx_leads_stage ON leads(stage);
CREATE INDEX idx_leads_manager ON leads(assigned_manager_id);
CREATE INDEX idx_leads_source ON leads(source);

CREATE TABLE audit_events (
    id BIGSERIAL PRIMARY KEY,
    actor_email VARCHAR(255) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    path VARCHAR(512) NOT NULL,
    status_code INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_created_at ON audit_events(created_at DESC);

ALTER TABLE students ALTER COLUMN fin DROP NOT NULL;
ALTER TABLE students ALTER COLUMN email DROP NOT NULL;
ALTER TABLE students ADD COLUMN source VARCHAR(30);
ALTER TABLE students ADD COLUMN parent_name VARCHAR(255);
ALTER TABLE students ADD COLUMN parent_phone VARCHAR(255);
ALTER TABLE students ADD COLUMN address VARCHAR(255);
ALTER TABLE students ADD COLUMN note TEXT;
ALTER TABLE students ADD COLUMN status VARCHAR(30) NOT NULL DEFAULT 'AKTIV';
ALTER TABLE students ADD COLUMN exit_reason TEXT;
CREATE UNIQUE INDEX uq_students_fin_upper ON students (upper(fin)) WHERE fin IS NOT NULL;
