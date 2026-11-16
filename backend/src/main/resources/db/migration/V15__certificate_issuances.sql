-- Permanent issue register, one certificate per student and completed group.
CREATE TABLE certificate_issuances(
  id BIGSERIAL PRIMARY KEY,
  student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE RESTRICT,
  group_id BIGINT NOT NULL REFERENCES course_groups(id) ON DELETE RESTRICT,
  certificate_title VARCHAR(255) NOT NULL,
  issued_on DATE NOT NULL,
  issued_by VARCHAR(255) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_certificate_student_group UNIQUE(student_id,group_id)
);
