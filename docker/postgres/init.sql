CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Drop in correct dependency order/ just in case the script is re-run during development
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users CASCADE;

CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  uuid          UUID DEFAULT gen_random_uuid() UNIQUE NOT NULL,
  email         VARCHAR(320) UNIQUE NOT NULL,
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
  code         VARCHAR(50) NOT NULL,
  description  TEXT,
  link         VARCHAR(1000),
  tags         VARCHAR(1000),
  material     TEXT,
  due_date     TIMESTAMPTZ,
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
CREATE INDEX idx_courses_code         ON courses(code);

CREATE TRIGGER trg_courses_updated_at
  BEFORE UPDATE ON courses
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE enrollments (
  id         SERIAL PRIMARY KEY,
  user_id    INTEGER NOT NULL,
  course_id  INTEGER NOT NULL,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_enrollments_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_enrollments_course
    FOREIGN KEY (course_id)
    REFERENCES courses(id)
    ON DELETE CASCADE,

  CONSTRAINT uq_enrollments_user_course UNIQUE (user_id, course_id)
);

CREATE INDEX idx_enrollments_user_id   ON enrollments(user_id);
CREATE INDEX idx_enrollments_course_id ON enrollments(course_id);

CREATE TABLE reviews (
  id         SERIAL PRIMARY KEY,
  user_id    INTEGER NOT NULL,
  course_id  INTEGER NOT NULL,
  rating     INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
  comment    TEXT,
  created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_reviews_user
    FOREIGN KEY (user_id)
    REFERENCES users(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_reviews_course
    FOREIGN KEY (course_id)
    REFERENCES courses(id)
    ON DELETE CASCADE
);

CREATE INDEX idx_reviews_user_id   ON reviews(user_id);
CREATE INDEX idx_reviews_course_id ON reviews(course_id);

CREATE TRIGGER trg_reviews_updated_at
  BEFORE UPDATE ON reviews
  FOR EACH ROW EXECUTE FUNCTION update_updated_at();
