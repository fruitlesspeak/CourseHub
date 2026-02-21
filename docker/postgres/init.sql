CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

-- Drop in correct dependency order/ just in case the script is re-run during development
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  uuid          UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
  email         CITEXT UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name    VARCHAR(100),
  last_name     VARCHAR(100),
  is_professor  BOOLEAN DEFAULT FALSE NOT NULL,
  professor_id  INTEGER,
  student_id    VARCHAR(50),
  created_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_users_professor
    FOREIGN KEY (professor_id)
    REFERENCES users(id)
    ON DELETE SET NULL,

  -- A professor cannot have a student_id
  CONSTRAINT check_role_consistency CHECK (
    (is_professor = TRUE  AND student_id IS NULL)
    OR
    (is_professor = FALSE)
  )
);

CREATE INDEX idx_users_is_professor ON users(is_professor);
CREATE INDEX idx_users_professor_id ON users(professor_id);
CREATE INDEX idx_users_student_id   ON users(student_id);

-- Generic updated_at trigger function (reusable)
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = CURRENT_TIMESTAMP;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
  BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE courses (
  id           SERIAL PRIMARY KEY,
  uuid         UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
  title        VARCHAR(255) NOT NULL,
  description  TEXT,
  professor_id INTEGER NOT NULL,
  created_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at   TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_courses_professor
    FOREIGN KEY (professor_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);

CREATE INDEX idx_courses_professor_id ON courses(professor_id);
CREATE INDEX idx_courses_title        ON courses(title);

CREATE TRIGGER trg_courses_updated_at
  BEFORE UPDATE ON courses
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();