package com.hospital.opd.repository;

import com.hospital.opd.model.OpdAppointment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface OpdAppointmentRepository extends JpaRepository<OpdAppointment, UUID> {
    // Standard JPA save, find, and delete methods are inherited automatically
}
