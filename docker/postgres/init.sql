-- Initialize database tables (optional)
-- This runs automatically when the container starts

CREATE TABLE IF NOT EXISTS example (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO example (name) VALUES ('Initial row') ON CONFLICT DO NOTHING;
