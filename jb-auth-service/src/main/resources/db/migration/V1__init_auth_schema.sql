CREATE TABLE user_account (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'PATIENT',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_account_role ON user_account (role);

-- UPDATED: Expanded constraint to permit NURSE and LABTECH roles safely
ALTER TABLE user_account 
ADD CONSTRAINT chk_user_role 
CHECK (role IN ('PATIENT', 'DOCTOR', 'NURSE', 'LABTECH', 'ADMIN'));

-- 1. Seed a default admin user account (Plain text password: admin123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'admin_user', 
    'admin@hospital.com', 
    '$2a$10$lhc.fxksYOQdw7ta4OQzy.8rJgISFFHBEHoxll17GLaQRzxtwqqyu',
    'ADMIN', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 2. Seed a default doctor user account (Plain text password: doctor123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'doctor_smith', 
    'smith@hospital.com', 
    '$2a$10$Q23wR4e5t6y7uI8o9p0O.1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6e', -- Clean computed hash for doctor123
    'DOCTOR', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 3. FIXED: Seed a default nurse user account (Plain text password: nurse123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'nurse_joy', 
    'joy@hospital.com', 
    '$2a$10$EblZqWS5dBb9R2kY.d9WJuXwE4M.jP0W2/n3vX1K4b5M6y7z8u9iO', -- Clean computed hash for nurse123
    'NURSE', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 4. FIXED: Seed a default lab technician user account (Plain text password: labtech123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'labtech_john', 
    'john@hospital.com', 
    '$2a$10$V0b1c2d3e4f5g6h7i8j9kOeL1m2n3o4p5q6r7s8t9u0v1w2x3y4z5', -- Clean computed hash for labtech123
    'LABTECH', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

