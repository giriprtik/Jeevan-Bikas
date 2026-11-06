package com.hospital.opd.dto;

import com.hospital.opd.model.Gender;
import com.hospital.opd.model.Patient;
import java.time.LocalDate;

/** Search result row for the returning-patient lookup. */
public record PatientSummary(
        Long id,
        String mrn,
        String fullName,
        LocalDate dateOfBirth,
        boolean dobEstimated,
        Gender gender,
        String phone,
        String bloodGroup,
        String allergies,
        boolean hasActiveVisit) {

    public static PatientSummary of(Patient p, boolean hasActiveVisit) {
        return new PatientSummary(p.getId(), p.getMrn(), p.getFullName(), p.getDateOfBirth(),
                p.isDobEstimated(), p.getGender(), p.getPhone(), p.getBloodGroup(), p.getAllergies(),
                hasActiveVisit);
    }
}
