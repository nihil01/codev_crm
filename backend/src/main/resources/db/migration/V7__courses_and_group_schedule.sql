CREATE TABLE courses (
 id BIGSERIAL PRIMARY KEY,
 name VARCHAR(255) NOT NULL UNIQUE,
 description TEXT,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE course_groups ADD COLUMN course_id BIGINT REFERENCES courses(id);
ALTER TABLE course_groups ADD COLUMN lesson_days VARCHAR(255);
ALTER TABLE course_groups ADD COLUMN lesson_time VARCHAR(255);
ALTER TABLE course_groups ADD COLUMN training_mode VARCHAR(20);
ALTER TABLE course_groups ADD COLUMN room VARCHAR(255);
ALTER TABLE course_groups ADD COLUMN max_students INTEGER;
ALTER TABLE course_groups ADD CONSTRAINT max_students_positive CHECK(max_students IS NULL OR max_students > 0);
CREATE INDEX idx_course_groups_course ON course_groups(course_id);
