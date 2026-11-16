-- Optional self-service access for teachers. No existing users are automatically upgraded.
ALTER TABLE users ADD COLUMN teacher_id BIGINT REFERENCES teachers(id);
CREATE UNIQUE INDEX uq_user_teacher_id ON users(teacher_id) WHERE teacher_id IS NOT NULL;
