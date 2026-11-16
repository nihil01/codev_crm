-- Preserve historical teacher assignments when a mentor leaves a group.
ALTER TABLE group_teachers ADD COLUMN effective_until_year INTEGER;
ALTER TABLE group_teachers ADD COLUMN effective_until_month INTEGER;
ALTER TABLE group_teachers ADD CONSTRAINT ck_teacher_assignment_end CHECK (
 (effective_until_year IS NULL AND effective_until_month IS NULL)
 OR (effective_until_year IS NOT NULL AND effective_until_month BETWEEN 1 AND 12)
);
