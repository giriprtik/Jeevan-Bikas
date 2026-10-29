package com.hospital.opd.repository;

import com.hospital.opd.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    // Used by service layers to perform duplication guardrail checks before registration
    Optional<Patient> findByPhone(String phone);
    
    // Used to retrieve patient master context by clinical file numbers
    Optional<Patient> findByMrn(String mrn);

    /**
     * Atomically extracts the next value from your database counter sequence.
     * Essential for formatting the standard medical record profile codes (e.g. JB-000001)
     */
    @Query(value = "SELECT nextval('patient_mrn_seq')", nativeQuery = true)
    Long getNextMrnSequenceValue();
}
