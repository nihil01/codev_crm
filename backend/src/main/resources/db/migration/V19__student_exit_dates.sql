-- Departure takes effect at the next full calendar month for billing.
ALTER TABLE students ADD COLUMN exit_date DATE;
UPDATE students SET exit_date=CURRENT_DATE WHERE status='AYRILIB' AND exit_date IS NULL;
