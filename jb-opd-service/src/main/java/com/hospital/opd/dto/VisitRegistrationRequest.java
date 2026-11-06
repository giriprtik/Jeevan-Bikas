package com.hospital.opd.dto;

import com.hospital.opd.model.VisitPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** New visit for a patient who is already registered (returning patient). */
public record VisitRegistrationRequest(
        @NotBlank(message = "is required") String patientMrn,
        @NotBlank(message = "is required") @Size(max = 500) String chiefComplaint,
        @NotNull(message = "is required (NORMAL, URGENT or EMERGENCY)") VisitPriority priority,
        @NotBlank(message = "is required") @Size(max = 50) String assignedDoctorUsername) {}
