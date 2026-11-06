-- Denormalised patient name so the lab worklist is readable without calling the OPD service.
ALTER TABLE lab_orders ADD COLUMN patient_name VARCHAR(150);
