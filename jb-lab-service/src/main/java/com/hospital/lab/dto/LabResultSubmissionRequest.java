package com.hospital.lab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LabResultSubmissionRequest {

    @NotNull(message = "Associated Lab Order ID reference link is required")
    private Long orderId;

    @NotBlank(message = "Target master lookup test code is required (e.g., CBC, LFT)")
    private String testCode;

    @NotBlank(message = "Clinical parameter valuation name is required (e.g., Hemoglobin, WBC)")
    @Size(max = 100)
    private String parameterName;

    @NotBlank(message = "Observed clinic measurement evaluation string value is required")
    @Size(max = 50)
    private String observedValue;

    @Size(max = 100)
    private String referenceRange;

    private boolean abnormal = false;

    private String technicianNotes;

    // Getters
    public Long getOrderId() { return orderId; }
    public String getTestCode() { return testCode; }
    public String getParameterName() { return parameterName; }
    public String getObservedValue() { return observedValue; }
    public String getReferenceRange() { return referenceRange; }
    public boolean isAbnormal() { return abnormal; }
    public String getTechnicianNotes() { return technicianNotes; }

    // Setters
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setTestCode(String testCode) { this.testCode = testCode; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public void setObservedValue(String observedValue) { this.observedValue = observedValue; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public void setAbnormal(boolean abnormal) { this.abnormal = abnormal; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }
}
