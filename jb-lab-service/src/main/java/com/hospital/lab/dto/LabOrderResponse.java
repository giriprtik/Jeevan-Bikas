package com.hospital.lab.dto;

import com.hospital.lab.model.LabOrderStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record LabOrderResponse(
        Long id,
        Long visitId,
        String patientMrn,
        String patientName,
        String orderedByDoctor,
        LabOrderStatus orderStatus,
        String clinicalHistory,
        OffsetDateTime orderedAt,
        List<String> testCodes,
        /** One entry per ordered test, in order. */
        List<Sample> samples) {

    public record Sample(String testCode, boolean collected, String collectedBy, OffsetDateTime collectedAt) {}
}
