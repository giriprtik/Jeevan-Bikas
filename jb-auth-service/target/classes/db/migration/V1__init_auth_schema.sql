CREATE TABLE user_account (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'PATIENT',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    full_name     VARCHAR(100) NOT NULL,                      -- Professional name (e.g., 'Dr. Ramesh Sharma')
    department    VARCHAR(50),                                -- e.g., 'GENERAL_OPD', 'PEDIATRICS', 'CARDIOLOGY'
    license_number VARCHAR(50) UNIQUE,                        -- Medical Council registration pointer (Optional for non-clinical)
    specialization VARCHAR(100)                               -- Clinical focus area details
);

CREATE INDEX idx_user_account_role ON user_account (role);

-- UPDATED: Expanded constraint to permit NURSE and LABTECH roles safely
ALTER TABLE user_account 
ADD CONSTRAINT chk_user_role 
CHECK (role IN ('DOCTOR', 'NURSE', 'LABTECH', 'ADMIN'));

