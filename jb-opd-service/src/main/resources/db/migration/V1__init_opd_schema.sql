-- 1. Create a table to register doctors inside the OPD scope
CREATE TABLE opd_doctor (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE
);

-- 2. Create the appointments management table
CREATE TABLE opd_appointment (
    id UUID PRIMARY KEY,
    patient_username VARCHAR(50) NOT NULL,
    doctor_id VARCHAR(50) NOT NULL,
    recorded_by VARCHAR(50) NOT NULL,
    preferred_time TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(30) DEFAULT 'Scheduled',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES opd_doctor(id)
);


CREATE TABLE patient_triage_vitals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    blood_pressure VARCHAR(20) NOT NULL,
    temperature_celsius NUMERIC(4,2) NOT NULL,
    pulse_rate INT NOT NULL,
    symptoms_brief TEXT,
    recorded_by VARCHAR(50) NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_vitals_appointment FOREIGN KEY (appointment_id) REFERENCES opd_appointment(id)
);

-- 3. Seed sample lookup records for verification testing
INSERT INTO opd_doctor (id, name, specialization, is_available) VALUES 
('DOC-77', 'Dr. Sarah Jenkins', 'Cardiology', TRUE),
('DOC-88', 'Dr. Robert Chen', 'Pediatrics', TRUE);

-- 4. ADD THESE LINES HERE TO OPTIMIZE YOUR APPOINTMENT LOOKUPS
CREATE INDEX idx_opd_appointment_patient ON opd_appointment (patient_username);
CREATE INDEX idx_opd_appointment_doctor ON opd_appointment (doctor_id);
CREATE INDEX idx_vitals_appointment ON patient_triage_vitals (appointment_id);

