CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Drop in correct dependency order/ just in case the script is re-run during development
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS important_dates;
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

CREATE TABLE important_dates (
  id                 SERIAL PRIMARY KEY,
  course_id          INTEGER NOT NULL,
  created_by_user_id INTEGER NOT NULL,
  title              VARCHAR(255) NOT NULL,
  description        TEXT,
  due_at             TIMESTAMPTZ NOT NULL,
  created_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
  updated_at         TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

  CONSTRAINT fk_important_dates_course
    FOREIGN KEY (course_id)
    REFERENCES courses(id)
    ON DELETE CASCADE,

  CONSTRAINT fk_important_dates_created_by_user
    FOREIGN KEY (created_by_user_id)
    REFERENCES users(id)
    ON DELETE CASCADE
);

CREATE INDEX idx_important_dates_course_due_at
  ON important_dates(course_id, due_at);
CREATE INDEX idx_important_dates_created_by_user
  ON important_dates(created_by_user_id);

CREATE TRIGGER trg_important_dates_updated_at
  BEFORE UPDATE ON important_dates
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

-- Demo accounts
INSERT INTO users (email, password_hash, first_name, last_name, is_professor, student_id)
VALUES
  ('prof.demo@coursehub.local', '$2a$10$rCvl0I3EhWWRETMhfF5IneSCjjk0tSrRQ8U2LtSPykhLJm.H.pzLu', 'Paula', 'Professor', TRUE, NULL),
  ('student.demo@coursehub.local', '$2a$10$rCvl0I3EhWWRETMhfF5IneSCjjk0tSrRQ8U2LtSPykhLJm.H.pzLu', 'Sam', 'Student', FALSE, 'S100001');

-- Demo courses owned by the professor account
INSERT INTO courses (title, code, description, tags, material, due_date, professor_id)
VALUES
  (
    'Introduction to Algorithms',
    'CS 301',
    'Core algorithm analysis, asymptotic notation, and problem solving techniques.',
    'algorithms,data structures',
    'Lecture slides, weekly practice sets, and worked examples.',
    '2026-03-23T09:00:00Z',
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local')
  ),
  (
    'Database Systems',
    'CS 420',
    'Relational design, SQL fundamentals, indexing, and transactions.',
    'databases,sql,backend',
    'Labs, schema design exercises, and sample queries.',
    '2026-03-24T14:00:00Z',
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local')
  ),
  (
    'Web Development',
    'CS 350',
    'Frontend and backend web application development foundations.',
    'web,frontend,backend',
    'Mini projects, starter code, and deployment notes.',
    '2026-03-26T10:00:00Z',
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local')
  ),
  (
    'Machine Learning',
    'CS 480',
    'Supervised learning, model evaluation, and practical ML workflows.',
    'machine learning,python,ai',
    'Notebook exercises and model evaluation reports.',
    '2026-03-28T16:00:00Z',
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local')
  );

-- Demo important dates for the student dashboard
INSERT INTO important_dates (course_id, created_by_user_id, title, description, due_at)
VALUES
  (
    (SELECT id FROM courses WHERE code = 'CS 301'),
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local'),
    'Algorithm Analysis Report',
    'Submit the complexity analysis report for the divide-and-conquer unit.',
    '2026-03-25T23:59:00Z'
  ),
  (
    (SELECT id FROM courses WHERE code = 'CS 420'),
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local'),
    'Database Design Checkpoint',
    'Bring your ER diagram draft and normalization notes to class.',
    '2026-03-27T13:00:00Z'
  ),
  (
    (SELECT id FROM courses WHERE code = 'CS 350'),
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local'),
    'Frontend Demo Review',
    'Present your interface draft and explain the main user flow.',
    '2026-03-29T11:30:00Z'
  ),
  (
    (SELECT id FROM courses WHERE code = 'CS 480'),
    (SELECT id FROM users WHERE email = 'prof.demo@coursehub.local'),
    'Model Evaluation Discussion',
    'Prepare notes comparing precision, recall, and F1 tradeoffs.',
    '2026-03-31T15:00:00Z'
  );
