package com.hospital.auth.dto;

import java.util.UUID;

/** Public, non-sensitive view of a staff account (used for doctor pickers and service-to-service lookups). */
public record StaffSummary(
        UUID id,
        String username,
        String fullName,
        String role,
        String department,
        String specialization) {}
