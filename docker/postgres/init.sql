-- Initialize CourseHub schema and seed user
-- Runs only when PostgreSQL data directory is first initialized.

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

-- Seed login user
-- Email: demo@student.coursehub
-- Password: CourseHub123!
INSERT INTO users (name, email, password_hash, role)
VALUES (
    'Demo Student',
    'demo@student.coursehub',
    '$2a$10$AowULxNmctcG.mYEPJm8vObGZhau9T43kMQc70RFowOaSo6ltLaP6',
    'STUDENT'
)
ON CONFLICT (email) DO NOTHING;
