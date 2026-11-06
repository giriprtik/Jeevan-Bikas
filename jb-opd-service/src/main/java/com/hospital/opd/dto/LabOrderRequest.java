package com.hospital.opd.dto;

import java.util.List;

/** Body sent to the lab service's POST /api/v1/lab/orders. Field names must match the lab DTO. */
public record LabOrderRequest(
        Long visitId,
        String patientMrn,
        String patientName,
        List<String> testCodes,
        String clinicalHistory) {}
