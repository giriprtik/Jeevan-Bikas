package com.hospital.opd.repository;

import com.hospital.opd.model.Patient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findFirstByPhone(String phone);

    Optional<Patient> findByMrn(String mrn);

    /** Returning-patient lookup by MRN, phone or name fragment. */
    @Query("select p from Patient p where upper(p.mrn) = upper(:q) or p.phone like concat('%', :q, '%') " +
           "or lower(p.fullName) like lower(concat('%', :q, '%')) order by p.createdAt desc")
    List<Patient> search(@Param("q") String q, Pageable page);

    List<Patient> findAllByOrderByCreatedAtDesc(Pageable page);

    @Query(value = "SELECT nextval('patient_mrn_seq')", nativeQuery = true)
    Long getNextMrnSequenceValue();
}
