package com.hospital.opd.dto;

import java.time.OffsetDateTime;

/** One result row from the lab service. */
public record LabResultItem(
        Long id,
        Long orderId,
        String testCode,
        String testName,
        String parameterName,
        String observedValue,
        String referenceRange,
        boolean abnormal,
        String technicianNotes,
        String resultEnteredBy,
        OffsetDateTime resultEnteredAt) {}