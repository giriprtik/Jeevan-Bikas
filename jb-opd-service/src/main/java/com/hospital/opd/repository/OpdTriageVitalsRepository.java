package com.hospital.opd.repository;

import com.hospital.opd.model.OpdTriageVitals;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OpdTriageVitalsRepository extends JpaRepository<OpdTriageVitals, UUID> {
    // Inherits standard database saving routines
    Optional<OpdTriageVitals> findByAppointmentId(UUID appointmentId);
}
