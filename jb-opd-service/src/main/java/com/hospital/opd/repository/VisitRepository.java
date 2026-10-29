package com.hospital.opd.repository;

import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    // Unique Constraint Validation Guardrail
    @Query("SELECT COUNT(v) > 0 FROM Visit v WHERE v.patient.id = :patientId AND v.status NOT IN ('COMPLETED', 'CANCELLED')")
    boolean hasActiveVisit(@Param("patientId") Long patientId);

    /**
     * Active Doctor's Queue Finder Query.
     * Pulls patients waiting for a specific doctor on the specified local date.
     */
    @Query("SELECT v FROM Visit v JOIN FETCH v.patient WHERE v.assignedDoctor = :doctorUsername " +
           "AND v.visitDate = :date " +
           "AND v.status = :status " +
           "ORDER BY v.tokenNumber ASC")
    List<Visit> findActiveQueue(
            @Param("doctorUsername") String doctorUsername,
            @Param("date") LocalDate date,
            @Param("status") VisitStatus status);
}
