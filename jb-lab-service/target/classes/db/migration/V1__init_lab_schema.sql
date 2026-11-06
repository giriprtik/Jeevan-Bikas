-- =============================================================================
-- LAB SERVICE DATABASE SCHEMA (Flyway / Versioned Migration-Ready)
-- Framework Configuration: Hibernate runs with ddl-auto=none
-- Core Domain Context: Lab Orders, Test Catalogs, and Results Entry
-- =============================================================================

-- 1. LAB TEST DICTIONARY (Master Lookup Catalog)
-- Holds valid tests like CBC, Liver Function Test (LFT), Fasting Blood Sugar, X-Ray, etc.
CREATE TABLE lab_tests (
    id          BIGSERIAL PRIMARY KEY,
    test_code   VARCHAR(20)  NOT NULL UNIQUE, -- e.g., 'CBC', 'LFT', 'FBS'
    test_name   VARCHAR(150) NOT NULL,
    department  VARCHAR(50)  NOT NULL,        -- e.g., 'HAEMATOLOGY', 'BIOCHEMISTRY'
    cost        NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);

-- 2. LAB ORDERS MASTER RECORD
CREATE TABLE lab_orders (
    id                  BIGSERIAL PRIMARY KEY,
    visit_id            BIGINT       NOT NULL, -- Cross-reference link matching your OPD visit ID
    patient_mrn         VARCHAR(20)  NOT NULL, -- Patient identification cross reference
    ordered_by_doctor   VARCHAR(50)  NOT NULL, -- Username of the requesting doctor
    order_status        VARCHAR(25)  NOT NULL CHECK (order_status IN ('PLACED', 'SAMPLE_COLLECTED', 'COMPLETED', 'CANCELLED')),
    clinical_history    TEXT,                  -- Brief provisional diagnosis from the doctor
    ordered_at          TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ
);

-- 3. LAB TEST RESULTS COMPONENT (Detailed parameter values)
CREATE TABLE lab_results (
    id                  BIGSERIAL PRIMARY KEY,
    order_id            BIGINT       NOT NULL REFERENCES lab_orders (id) ON DELETE CASCADE,
    test_id             BIGINT       NOT NULL REFERENCES lab_tests (id),
    parameter_name      VARCHAR(100) NOT NULL, -- e.g., 'Hemoglobin', 'WBC Count', 'Serum Bilirubin'
    observed_value      VARCHAR(50)  NOT NULL, -- e.g., '14.2', '7500', '1.1'
    reference_range     VARCHAR(100),          -- e.g., '13.5 - 17.5 g/dL', '4000 - 11000 /uL'
    is_abnormal         BOOLEAN      NOT NULL DEFAULT FALSE, -- Auto-flagged if out of reference range
    technician_notes    TEXT,
    result_entered_by   VARCHAR(50)  NOT NULL, -- Lab Technician's username
    result_entered_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- PERFORMANCE & LOOKUP OPTIMIZATIONS
-- =============================================================================
CREATE INDEX idx_lab_orders_visit ON lab_orders (visit_id);
CREATE INDEX idx_lab_orders_mrn ON lab_orders (patient_mrn);
CREATE INDEX idx_lab_orders_status ON lab_orders (order_status);
CREATE INDEX idx_lab_results_order ON lab_results (order_id);

-- Seed basic lookup catalog data right away
INSERT INTO lab_tests (test_code, test_name, department, cost) VALUES
('CBC', 'Complete Blood Count', 'HAEMATOLOGY', 350.00),
('LFT', 'Liver Function Test', 'BIOCHEMISTRY', 850.00),
('FBS', 'Fasting Blood Sugar', 'BIOCHEMISTRY', 120.00),
('RFT', 'Renal Function Test', 'BIOCHEMISTRY', 600.00);
