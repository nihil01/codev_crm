CREATE TABLE student_freeze_periods (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    freeze_year INTEGER NOT NULL,
    freeze_month INTEGER NOT NULL CHECK(freeze_month BETWEEN 1 AND 12),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_freeze_month UNIQUE(student_id,freeze_year,freeze_month)
);
CREATE INDEX idx_student_freeze_student ON student_freeze_periods(student_id);
