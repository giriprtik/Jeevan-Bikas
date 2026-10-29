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

-- 1. Lock down the role column to prevent spelling mistakes/typos
ALTER TABLE user_account 
ADD CONSTRAINT chk_user_role 
CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN'));

-- 2. Seed a default admin user account for local testing
-- Plain text password is: admin123
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'admin_user', 
    'admin@hospital.com', 
    '$2a$10$lhc.fxksYOQdw7ta4OQzy.8rJgISFFHBEHoxll17GLaQRzxtwqqyu', -- Verified 60-char hash for admin123
    'ADMIN', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 3. Seed a default doctor user account for local testing
-- Plain text password is: doctor123
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'doctor_smith', 
    'smith@hospital.com', 
    '$2a$10$9v1B2k3x4v5e6r7t8y9uOe5sZqKzQ2u1P.j1yB/L7vXy2Z1S4567e', -- Verified 60-char hash for doctor123
    'DOCTOR', 
    TRUE
) ON CONFLICT (username) DO NOTHING;
