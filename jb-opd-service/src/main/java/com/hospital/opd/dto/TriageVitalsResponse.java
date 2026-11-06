package com.hospital.opd.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TriageVitalsResponse {
    private Long id;
    private Long visitId;
    private String patientName; // Captured dynamically from the linked entity chain
    private BigDecimal temperatureCelsius;
    private Integer pulseRate;
    private String bloodPressure;
    private Integer oxygenSaturation;
    private String symptomsBrief;
    private String recordedBy;
    private OffsetDateTime recordedAt;

    // Updated Constructor with patientName parameter injection block
    public TriageVitalsResponse(Long id, Long visitId, String patientName, BigDecimal temperatureCelsius, 
                                Integer pulseRate, String bloodPressure, Integer oxygenSaturation,
                                String symptomsBrief, String recordedBy, OffsetDateTime recordedAt) {
        this.id = id;
        this.visitId = visitId;
        this.patientName = patientName; // Binds the structural string value cleanly
        this.temperatureCelsius = temperatureCelsius;
        this.pulseRate = pulseRate;
        this.bloodPressure = bloodPressure;
        this.oxygenSaturation = oxygenSaturation;
        this.symptomsBrief = symptomsBrief;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    // Standard Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public String getPatientName() { return patientName; } // ◄ Added Getter for frontend extraction
    public BigDecimal getTemperatureCelsius() { return temperatureCelsius; }
    public Integer getPulseRate() { return pulseRate; }
    public String getBloodPressure() { return bloodPressure; }
    public Integer getOxygenSaturation() { return oxygenSaturation; }
    public String getSymptomsBrief() { return symptomsBrief; }
    public String getRecordedBy() { return recordedBy; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
}
