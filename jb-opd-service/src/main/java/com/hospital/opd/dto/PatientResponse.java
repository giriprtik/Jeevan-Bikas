package com.hospital.opd.dto;

import com.hospital.opd.model.Gender;
import java.time.LocalDate;

/** Result of registering a patient: the new MRN plus the visit/token that was opened. */
public record PatientResponse(
        Long id,
        Long visitId,
        Integer tokenNumber,
        String mrn,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone) {}
