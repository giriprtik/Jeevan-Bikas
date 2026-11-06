package com.hospital.opd.dto;

import java.util.UUID;

/** Mirror of the auth service's StaffSummary (internal lookup response). */
public record StaffSummary(
        UUID id,
        String username,
        String fullName,
        String role,
        String department,
        String specialization) {}
