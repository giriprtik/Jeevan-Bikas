package com.hospital.lab.dto;

import java.time.OffsetDateTime;

public class LabResultResponse {
    private Long id;
    private Long orderId;
    private String testCode;
    private String testName;
    private String parameterName;
    private String observedValue;
    private String referenceRange;
    private boolean abnormal;
    private String technicianNotes;
    private String resultEnteredBy;
    private OffsetDateTime resultEnteredAt;

    public LabResultResponse(Long id, Long orderId, String testCode, String testName, 
                             String parameterName, String observedValue, String referenceRange, 
                             boolean abnormal, String technicianNotes, String resultEnteredBy, 
                             OffsetDateTime resultEnteredAt) {
        this.id = id;
        this.orderId = orderId;
        this.testCode = testCode;
        this.testName = testName;
        this.parameterName = parameterName;
        this.observedValue = observedValue;
        this.referenceRange = referenceRange;
        this.abnormal = abnormal;
        this.technicianNotes = technicianNotes;
        this.resultEnteredBy = resultEnteredBy;
        this.resultEnteredAt = resultEnteredAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getTestCode() { return testCode; }
    public String getTestName() { return testName; }
    public String getParameterName() { return parameterName; }
    public String getObservedValue() { return observedValue; }
    public String getReferenceRange() { return referenceRange; }
    public boolean isAbnormal() { return abnormal; }
    public String getTechnicianNotes() { return technicianNotes; }
    public String getResultEnteredBy() { return resultEnteredBy; }
    public OffsetDateTime getResultEnteredAt() { return resultEnteredAt; }
}
