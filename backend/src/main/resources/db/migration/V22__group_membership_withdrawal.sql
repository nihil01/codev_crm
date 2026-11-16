-- Preserve enrollment history after a student leaves a particular course.
ALTER TABLE group_students ADD COLUMN left_at DATE;
ALTER TABLE group_students ADD COLUMN exit_reason TEXT;
CREATE INDEX idx_group_student_active ON group_students(group_id,student_id)
  WHERE left_at IS NULL;
