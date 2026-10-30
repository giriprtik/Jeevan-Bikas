package com.hospital.lab.dto;

import com.hospital.lab.model.LabOrderStatus;
import java.time.OffsetDateTime;

public class LabOrderResponse {
    private Long id;
    private Long visitId;
    private String patientMrn;
    private String orderedByDoctor;
    private LabOrderStatus orderStatus;
    private String clinicalHistory;
    private OffsetDateTime orderedAt;

    public LabOrderResponse(Long id, Long visitId, String patientMrn, String orderedByDoctor, 
                            LabOrderStatus orderStatus, String clinicalHistory, OffsetDateTime orderedAt) {
        this.id = id;
        this.visitId = visitId;
        this.patientMrn = patientMrn;
        this.orderedByDoctor = orderedByDoctor;
        this.orderStatus = orderStatus;
        this.clinicalHistory = clinicalHistory;
        this.orderedAt = orderedAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public String getPatientMrn() { return patientMrn; }
    public String getOrderedByDoctor() { return orderedByDoctor; }
    public LabOrderStatus getOrderStatus() { return orderStatus; }
    public String getClinicalHistory() { return clinicalHistory; }
    public OffsetDateTime getOrderedAt() { return orderedAt; }
}
