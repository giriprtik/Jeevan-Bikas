package com.hospital.opd.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** Lab service's order as OPD sees it (subset of lab's LabOrderResponse). */
public record LabOrderSummary(
        Long id,
        Long visitId,
        String orderStatus,
        String orderedByDoctor,
        OffsetDateTime orderedAt,
        List<String> testCodes) {}