-- =============================================================================
-- OPD SERVICE DATABASE SCHEMA (Flyway / Versioned Migration-Ready)
-- Framework Configuration: Hibernate runs with ddl-auto=none
-- Core Domain Context: Normalized Patient & Visit Lifecycle Event Tracking
-- =============================================================================

-- 1. SEQUENCES & COUNTERS
-- Generates unique, predictable Medical Record Numbers chronologically (e.g., JB-000001, JB-000002)
CREATE SEQUENCE patient_mrn_seq START WITH 1 INCREMENT BY 1;

-- Daily OPD queue token numbers (1, 2, 3 ...). Managed atomically inside the service engine
CREATE TABLE daily_token_counters (
    counter_date DATE    PRIMARY KEY,
    last_value   INTEGER NOT NULL
);

-- 2. PATIENT MASTER RECORD
CREATE TABLE patients (
    id                      BIGSERIAL PRIMARY KEY,
    mrn                     VARCHAR(20)  NOT NULL UNIQUE,
    full_name               VARCHAR(150) NOT NULL,
    date_of_birth           DATE         NOT NULL,
    dob_estimated           BOOLEAN      NOT NULL DEFAULT FALSE, -- TRUE when age was calculated dynamically
    gender                  VARCHAR(10)  NOT NULL CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    phone                   VARCHAR(20)  NOT NULL,
    address                 VARCHAR(255),
    blood_group             VARCHAR(5),
    allergies               TEXT,
    emergency_contact_name  VARCHAR(150),
    emergency_contact_phone VARCHAR(20),
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              VARCHAR(50)  NOT NULL,
    updated_at              TIMESTAMPTZ,
    version                 BIGINT       NOT NULL DEFAULT 0
);

-- 3. PATIENT CLINICAL VISITS LAYER
CREATE TABLE visits (
    id                      BIGSERIAL PRIMARY KEY,
    patient_id              BIGINT      NOT NULL REFERENCES patients (id),
    visit_date              DATE        NOT NULL,
    token_number            INTEGER     NOT NULL,
    chief_complaint         TEXT        NOT NULL,
    priority                VARCHAR(15) NOT NULL CHECK (priority IN ('NORMAL', 'URGENT', 'EMERGENCY')),
    status                  VARCHAR(25) NOT NULL CHECK (status IN ('REGISTERED', 'TRIAGED', 'IN_CONSULTATION',
                                                                    'AWAITING_LAB', 'LAB_RESULTS_READY', 'COMPLETED', 'CANCELLED')),
    assigned_doctor         VARCHAR(50),  -- Captured directly from Auth token usernames (e.g. 'doctor_smith')
    assigned_doctor_name    VARCHAR(150), -- Decoupled descriptive name for reporting convenience
    consultation_notes      TEXT,
    diagnosis               TEXT,
    latest_lab_order_id     BIGINT,
    cancel_reason           TEXT,
    registered_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registered_by           VARCHAR(50) NOT NULL,
    consultation_started_at TIMESTAMPTZ,
    completed_at            TIMESTAMPTZ,
    version                 BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_visits_token_per_day UNIQUE (visit_date, token_number)
);

-- 4. TRIAGE CLINICAL VITAL SIGNS
CREATE TABLE vital_signs (
    id                  BIGSERIAL PRIMARY KEY,
    visit_id            BIGINT       NOT NULL REFERENCES visits (id),
    temperature_celsius NUMERIC(4, 2) NOT NULL,
    pulse_rate          INTEGER      NOT NULL,
    blood_pressure      VARCHAR(20)  NOT NULL,
    symptoms_brief      TEXT,
    recorded_by         VARCHAR(50)  NOT NULL,
    recorded_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. COMPLIANCE AUDITING SYSTEMS
-- Append-only tracking engine archiving every mutation step a patient experiences 
CREATE TABLE visit_events (
    id          BIGSERIAL PRIMARY KEY,
    visit_id    BIGINT      NOT NULL REFERENCES visits (id),
    from_status VARCHAR(25),
    to_status   VARCHAR(25) NOT NULL,
    actor       VARCHAR(50) NOT NULL,
    note        TEXT,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- PERFORMANCE & ENFORCEMENT OPTIMIZATIONS (Indexes and Partial Constraints)
-- =============================================================================
CREATE INDEX idx_patients_phone ON patients (phone);
CREATE INDEX idx_patients_name_lower ON patients (lower(full_name));
CREATE INDEX idx_visits_date_status ON visits (visit_date, status);
CREATE INDEX idx_visits_patient ON visits (patient_id);
CREATE INDEX idx_vital_signs_visit ON vital_signs (visit_id, recorded_at DESC);
CREATE INDEX idx_visit_events_visit ON visit_events (visit_id, occurred_at);

-- Guardrail Index: Prevents a patient from opening overlapping visits concurrently
CREATE UNIQUE INDEX uq_visits_one_active_per_patient ON visits (patient_id)
    WHERE status NOT IN ('COMPLETED', 'CANCELLED');
