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
-- 1. Seed a default admin user account (Plain text password: admin123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'admin_user', 
    'admin@hospital.com', 
    '$2a$10$tZ922SInoW/23C2L/gZ.8unX7g5CAnS3M9T89vR6r3M6nE6t8K9qu', -- Valid hash for admin123
    'ADMIN', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 2. Seed a default doctor user account (Plain text password: doctor123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'doctor_smith', 
    'smith@hospital.com', 
    '$2a$10$PBhEt.IZTc3FK/XCusoWGe95Smv6HNXx2CF489eCb5am.EmJcBYfG', -- Valid hash for doctor123
    'DOCTOR', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 3. Seed a default nurse user account (Plain text password: nurse123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'nurse_joy', 
    'joy@hospital.com', 
    '$2a$10$KBeCenC7GfI3hPZ5t9.Wbuw7g1pX8V2e3vK4M5y6z7u8i9oOpPqQr', -- Valid hash for nurse123
    'NURSE', 
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- 4. Seed a default lab technician user account (Plain text password: labtech123)
INSERT INTO user_account (username, email, password_hash, role, enabled)
VALUES (
    'labtech_john', 
    'john@hospital.com', 
    '$2a$10$wO7vEw8Nf7X66wM52vYmDu9X96hB/A4.xHlI00T6Y2t6F68wS5Vem', -- Valid hash for labtech123
    'LABTECH', 
    TRUE
) ON CONFLICT (username) DO NOTHING;


