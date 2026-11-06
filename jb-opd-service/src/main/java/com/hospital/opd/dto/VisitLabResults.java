package com.hospital.opd.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Lab order + results for a visit, returned to the doctor's screen. */
public record VisitLabResults(
        Long orderId,
        String orderStatus,
        String orderedByDoctor,
        OffsetDateTime orderedAt,
        List<String> testCodes,
        List<LabResultItem> results) {}