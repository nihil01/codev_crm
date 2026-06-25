-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Teachers table
CREATE TABLE IF NOT EXISTS teachers (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    fin VARCHAR(7) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    gmail VARCHAR(255),
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'AKTIV',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Course groups table
CREATE TABLE IF NOT EXISTS course_groups (
    id BIGSERIAL PRIMARY KEY,
    group_number VARCHAR(50) NOT NULL UNIQUE,
    start_date DATE NOT NULL,
    duration_months INTEGER NOT NULL,
    end_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'AKTIV',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Students table
CREATE TABLE IF NOT EXISTS students (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    fin VARCHAR(7) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    gmail VARCHAR(255),
    phone VARCHAR(50),
    group_id BIGINT REFERENCES course_groups(id),
    registration_date DATE NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Group-Student junction table
CREATE TABLE IF NOT EXISTS group_students (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES course_groups(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    monthly_amount DECIMAL(12,2) NOT NULL,
    join_date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(group_id, student_id)
);

-- Group-Teacher junction table
CREATE TABLE IF NOT EXISTS group_teachers (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES course_groups(id) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    salary_percentage DECIMAL(5,2) NOT NULL,
    calculated_salary DECIMAL(12,2),
    salary_status VARCHAR(50),
    effective_month INTEGER NOT NULL,
    effective_year INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Payments table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    group_id BIGINT NOT NULL REFERENCES course_groups(id) ON DELETE CASCADE,
    payment_year INTEGER NOT NULL,
    payment_month INTEGER NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    payment_date DATE,
    payment_method VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'GOZLENILIR',
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Expenses table
CREATE TABLE IF NOT EXISTS expenses (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    expense_year INTEGER NOT NULL,
    expense_month INTEGER NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_students_group ON students(group_id);
CREATE INDEX idx_students_fin ON students(fin);
CREATE INDEX idx_payments_student ON payments(student_id);
CREATE INDEX idx_payments_group ON payments(group_id);
CREATE INDEX idx_payments_period ON payments(payment_year, payment_month);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_group_students_group ON group_students(group_id);
CREATE INDEX idx_group_students_student ON group_students(student_id);
CREATE INDEX idx_group_teachers_group ON group_teachers(group_id);
CREATE INDEX idx_expenses_period ON expenses(expense_year, expense_month);
