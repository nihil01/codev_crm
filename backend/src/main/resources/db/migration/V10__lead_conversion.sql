ALTER TABLE leads ADD COLUMN converted_student_id BIGINT REFERENCES students(id);
CREATE UNIQUE INDEX uq_lead_converted_student ON leads(converted_student_id)
  WHERE converted_student_id IS NOT NULL;
