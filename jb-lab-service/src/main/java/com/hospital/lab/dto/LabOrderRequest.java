package com.hospital.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class LabOrderRequest {

    @NotNull(message = "Visit ID reference is required")
    private Long visitId;

    @NotBlank(message = "Patient MRN is required")
    private String patientMrn;

    private String clinicalHistory;

    // Getters and Setters
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getPatientMrn() { return patientMrn; }
    public void setPatientMrn(String patientMrn) { this.patientMrn = patientMrn; }
    public String getClinicalHistory() { return clinicalHistory; }
    public void setClinicalHistory(String clinicalHistory) { this.clinicalHistory = clinicalHistory; }
}
