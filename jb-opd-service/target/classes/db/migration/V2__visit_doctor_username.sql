-- Doctor queues are keyed by the doctor's username (the JWT subject), so the OPD service
-- can filter "my patients" without a round-trip to the auth service on every request.
ALTER TABLE visits ADD COLUMN assigned_doctor_username VARCHAR(50);

CREATE INDEX idx_visits_doctor_status ON visits (assigned_doctor_username, status);
