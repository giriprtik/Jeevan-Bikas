-- Missing tests the doctor can order
INSERT INTO lab_tests (test_code, test_name, department, cost) VALUES
('LIPID', 'Lipid Profile', 'BIOCHEMISTRY', 700.00),
('URINE', 'Urinalysis',   'PATHOLOGY',    200.00)
ON CONFLICT (test_code) DO NOTHING;

-- Parameter catalogue with adult reference ranges (NULL = no numeric range / qualitative)
CREATE TABLE lab_test_parameters (
    id             BIGSERIAL PRIMARY KEY,
    test_id        BIGINT        NOT NULL REFERENCES lab_tests (id) ON DELETE CASCADE,
    parameter_name VARCHAR(100)  NOT NULL,
    unit           VARCHAR(30),
    ref_low        NUMERIC(12,3),
    ref_high       NUMERIC(12,3),
    display_order  INT           NOT NULL DEFAULT 0,
    UNIQUE (test_id, parameter_name)
);

INSERT INTO lab_test_parameters (test_id, parameter_name, unit, ref_low, ref_high, display_order)
SELECT t.id, v.name, v.unit, v.lo, v.hi, v.ord
FROM lab_tests t
JOIN (VALUES
    ('CBC',   'Hemoglobin',          'g/dL',       12.0::numeric, 17.5::numeric, 1),
    ('CBC',   'Total WBC Count',     'x10^3/uL',   4.0,   11.0,  2),
    ('CBC',   'RBC Count',           'x10^6/uL',   4.0,   6.0,   3),
    ('CBC',   'Hematocrit',          '%',          36.0,  52.0,  4),
    ('CBC',   'MCV',                 'fL',         80.0,  100.0, 5),
    ('CBC',   'Platelet Count',      'x10^3/uL',   150.0, 450.0, 6),
    ('CBC',   'Neutrophils',         '%',          40.0,  75.0,  7),
    ('CBC',   'Lymphocytes',         '%',          20.0,  45.0,  8),

    ('LFT',   'Total Bilirubin',     'mg/dL',      0.2,   1.2,   1),
    ('LFT',   'Direct Bilirubin',    'mg/dL',      0.0,   0.3,   2),
    ('LFT',   'AST (SGOT)',          'U/L',        5.0,   40.0,  3),
    ('LFT',   'ALT (SGPT)',          'U/L',        7.0,   56.0,  4),
    ('LFT',   'Alkaline Phosphatase','U/L',        44.0,  147.0, 5),
    ('LFT',   'Total Protein',       'g/dL',       6.0,   8.3,   6),
    ('LFT',   'Albumin',             'g/dL',       3.5,   5.0,   7),

    ('FBS',   'Fasting Glucose',     'mg/dL',      70.0,  100.0, 1),

    ('RFT',   'Urea',                'mg/dL',      15.0,  40.0,  1),
    ('RFT',   'Creatinine',          'mg/dL',      0.6,   1.3,   2),
    ('RFT',   'Sodium',              'mmol/L',     135.0, 145.0, 3),
    ('RFT',   'Potassium',           'mmol/L',     3.5,   5.1,   4),
    ('RFT',   'Uric Acid',           'mg/dL',      3.5,   7.2,   5),

    ('LIPID', 'Total Cholesterol',   'mg/dL',      NULL,  200.0, 1),
    ('LIPID', 'Triglycerides',       'mg/dL',      NULL,  150.0, 2),
    ('LIPID', 'HDL Cholesterol',     'mg/dL',      40.0,  NULL,  3),
    ('LIPID', 'LDL Cholesterol',     'mg/dL',      NULL,  100.0, 4),

    ('URINE', 'pH',                  NULL,         4.5,   8.0,   1),
    ('URINE', 'Specific Gravity',    NULL,         1.005, 1.030, 2),
    ('URINE', 'Protein',             NULL,         NULL,  NULL,  3),
    ('URINE', 'Glucose',             NULL,         NULL,  NULL,  4),
    ('URINE', 'Pus Cells',           '/HPF',       0.0,   5.0,   5),
    ('URINE', 'RBC',                 '/HPF',       0.0,   2.0,   6)
) AS v(code, name, unit, lo, hi, ord) ON v.code = t.test_code;