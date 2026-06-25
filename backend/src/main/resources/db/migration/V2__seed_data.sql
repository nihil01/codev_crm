-- Users are created by DataInitializer with BCrypt encoding
-- Sample teachers
INSERT INTO teachers (first_name, last_name, fin, email, gmail, phone, status) VALUES
('Əli', 'Həsənov', 'ABC1234', 'ali@tedris.az', 'ali.hesnov@gmail.com', '+994501234567', 'AKTIV'),
('Leyla', 'Məmmədova', 'DEF5678', 'leyla@tedris.az', 'leyla.mammadova@gmail.com', '+994512345678', 'AKTIV'),
('Kamran', 'Əliyev', 'GHI9012', 'kamran@tedris.az', 'kamran.aliyev@gmail.com', '+994553456789', 'AKTIV')
ON CONFLICT (fin) DO NOTHING;

-- Sample course group
INSERT INTO course_groups (group_number, start_date, duration_months, end_date, status) VALUES
('G-2026-01', '2026-01-10', 6, '2026-07-10', 'AKTIV'),
('G-2026-02', '2026-03-01', 8, '2026-11-01', 'AKTIV'),
('G-2025-03', '2025-09-15', 10, '2026-07-15', 'AKTIV')
ON CONFLICT (group_number) DO NOTHING;

-- Sample students
INSERT INTO students (first_name, last_name, fin, email, gmail, phone, registration_date, archived) VALUES
('Tural', 'Quliyev', 'TQR1234', 'tural@tedris.az', 'tural.guliyev@gmail.com', '+994701112233', '2026-01-10', FALSE),
('Nigar', 'İsmayılova', 'NII5678', 'nigar@tedris.az', 'nigar.ismayilova@gmail.com', '+994702223344', '2026-01-10', FALSE),
('Orxan', 'Babayev', 'OBB9012', 'orxan@tedris.az', 'orxan.babayev@gmail.com', '+994703334455', '2026-01-12', FALSE),
('Aynur', 'Rəhimova', 'AAR3456', 'aynur@tedris.az', 'aynur.rahimova@gmail.com', '+994704445566', '2026-03-01', FALSE),
('Elvin', 'Məmmədov', 'EMM7890', 'elvin@tedris.az', 'elvin.mammadov@gmail.com', '+994705556677', '2026-03-02', FALSE),
('Günel', 'Əsgərova', 'GAA2345', 'gunel@tedris.az', 'gunel.asgarova@gmail.com', '+994706667788', '2025-09-15', FALSE)
ON CONFLICT (fin) DO NOTHING;

-- Assign students to groups
INSERT INTO group_students (group_id, student_id, monthly_amount, join_date) VALUES
(1, 1, 200.00, '2026-01-10'),
(1, 2, 150.00, '2026-01-10'),
(1, 3, 250.00, '2026-01-12'),
(2, 4, 180.00, '2026-03-01'),
(2, 5, 220.00, '2026-03-02'),
(3, 6, 200.00, '2025-09-15')
ON CONFLICT (group_id, student_id) DO NOTHING;

-- Assign teachers to groups
INSERT INTO group_teachers (group_id, teacher_id, salary_percentage, effective_month, effective_year) VALUES
(1, 1, 30.00, 1, 2026),
(2, 2, 25.00, 3, 2026),
(3, 3, 35.00, 9, 2025)
ON CONFLICT DO NOTHING;

-- Generate payment schedules for Group 1 (G-2026-01, Jan 2026 - Jul 2026)
INSERT INTO payments (student_id, group_id, payment_year, payment_month, amount, status) VALUES
-- Tural (200 AZN/month)
(1, 1, 2026, 1, 200.00, 'ODENILIB'),
(1, 1, 2026, 2, 200.00, 'ODENILIB'),
(1, 1, 2026, 3, 200.00, 'ODENILIB'),
(1, 1, 2026, 4, 200.00, 'ODENILIB'),
(1, 1, 2026, 5, 200.00, 'ODENILIB'),
(1, 1, 2026, 6, 200.00, 'GOZLENILIR'),
(1, 1, 2026, 7, 200.00, 'GOZLENILIR'),
-- Nigar (150 AZN/month)
(2, 1, 2026, 1, 150.00, 'ODENILIB'),
(2, 1, 2026, 2, 150.00, 'ODENILIB'),
(2, 1, 2026, 3, 150.00, 'ODENILIB'),
(2, 1, 2026, 4, 150.00, 'GECIKIB'),
(2, 1, 2026, 5, 150.00, 'GOZLENILIR'),
(2, 1, 2026, 6, 150.00, 'GOZLENILIR'),
(2, 1, 2026, 7, 150.00, 'GOZLENILIR'),
-- Orxan (250 AZN/month)
(3, 1, 2026, 1, 250.00, 'ODENILIB'),
(3, 1, 2026, 2, 250.00, 'ODENILIB'),
(3, 1, 2026, 3, 250.00, 'ODENILIB'),
(3, 1, 2026, 4, 250.00, 'ODENILIB'),
(3, 1, 2026, 5, 250.00, 'ODENILIB'),
(3, 1, 2026, 6, 250.00, 'GOZLENILIR'),
(3, 1, 2026, 7, 250.00, 'GOZLENILIR');

-- Generate payment schedules for Group 2 (G-2026-02, Mar 2026 - Nov 2026)
INSERT INTO payments (student_id, group_id, payment_year, payment_month, amount, status) VALUES
-- Aynur (180 AZN/month)
(4, 2, 2026, 3, 180.00, 'ODENILIB'),
(4, 2, 2026, 4, 180.00, 'ODENILIB'),
(4, 2, 2026, 5, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 6, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 7, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 8, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 9, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 10, 180.00, 'GOZLENILIR'),
(4, 2, 2026, 11, 180.00, 'GOZLENILIR'),
-- Elvin (220 AZN/month)
(5, 2, 2026, 3, 220.00, 'ODENILIB'),
(5, 2, 2026, 4, 220.00, 'GECIKIB'),
(5, 2, 2026, 5, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 6, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 7, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 8, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 9, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 10, 220.00, 'GOZLENILIR'),
(5, 2, 2026, 11, 220.00, 'GOZLENILIR');

-- Generate payment schedules for Group 3 (G-2025-03, Sep 2025 - Jul 2026)
INSERT INTO payments (student_id, group_id, payment_year, payment_month, amount, status) VALUES
-- Günel (200 AZN/month)
(6, 3, 2025, 9, 200.00, 'ODENILIB'),
(6, 3, 2025, 10, 200.00, 'ODENILIB'),
(6, 3, 2025, 11, 200.00, 'ODENILIB'),
(6, 3, 2025, 12, 200.00, 'ODENILIB'),
(6, 3, 2026, 1, 200.00, 'ODENILIB'),
(6, 3, 2026, 2, 200.00, 'ODENILIB'),
(6, 3, 2026, 3, 200.00, 'ODENILIB'),
(6, 3, 2026, 4, 200.00, 'ODENILIB'),
(6, 3, 2026, 5, 200.00, 'ODENILIB'),
(6, 3, 2026, 6, 200.00, 'GOZLENILIR'),
(6, 3, 2026, 7, 200.00, 'GOZLENILIR');

-- Sample expenses (recurring)
INSERT INTO expenses (type, amount, expense_year, expense_month, is_recurring, description) VALUES
('YER', 500.00, 2026, 1, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 1, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 1, TRUE, 'İnternet paketi'),
('YER', 500.00, 2026, 2, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 2, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 2, TRUE, 'İnternet paketi'),
('YER', 500.00, 2026, 3, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 3, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 3, TRUE, 'İnternet paketi'),
('YER', 500.00, 2026, 4, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 4, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 4, TRUE, 'İnternet paketi'),
('YER', 500.00, 2026, 5, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 5, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 5, TRUE, 'İnternet paketi'),
('YER', 500.00, 2026, 6, TRUE, 'Ofis icarəsi'),
('ISCHI_MAASI', 800.00, 2026, 6, TRUE, 'İnzibati heyət maaşı'),
('INTERNET', 80.00, 2026, 6, TRUE, 'İnternet paketi');

-- Sample utility expenses (variable)
INSERT INTO expenses (type, amount, expense_year, expense_month, is_recurring, description) VALUES
('ELEKTRIK', 120.00, 2026, 1, FALSE, 'Janvar elektrik'),
('SU', 45.00, 2026, 1, FALSE, 'Janvar su'),
('QAZ', 60.00, 2026, 1, FALSE, 'Janvar qaz'),
('ELEKTRIK', 135.00, 2026, 2, FALSE, 'Fevral elektrik'),
('SU', 40.00, 2026, 2, FALSE, 'Fevral su'),
('QAZ', 70.00, 2026, 2, FALSE, 'Fevral qaz'),
('ELEKTRIK', 110.00, 2026, 3, FALSE, 'Mart elektrik'),
('SU', 50.00, 2026, 3, FALSE, 'Mart su'),
('QAZ', 55.00, 2026, 3, FALSE, 'Mart qaz'),
('ELEKTRIK', 125.00, 2026, 4, FALSE, 'Aprel elektrik'),
('SU', 42.00, 2026, 4, FALSE, 'Aprel su'),
('ELEKTRIK', 140.00, 2026, 5, FALSE, 'May elektrik'),
('SU', 38.00, 2026, 5, FALSE, 'May su');
