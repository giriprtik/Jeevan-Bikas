package com.hospital.opd.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TriageVitalsResponse {
    private Long id;
    private Long visitId;
    private BigDecimal temperatureCelsius;
    private Integer pulseRate;
    private String bloodPressure;
    private String symptomsBrief;
    private String recordedBy;
    private OffsetDateTime recordedAt;

    public TriageVitalsResponse(Long id, Long visitId, BigDecimal temperatureCelsius, 
                                Integer pulseRate, String bloodPressure, String symptomsBrief, 
                                String recordedBy, OffsetDateTime recordedAt) {
        this.id = id;
        this.visitId = visitId;
        this.temperatureCelsius = temperatureCelsius;
        this.pulseRate = pulseRate;
        this.bloodPressure = bloodPressure;
        this.symptomsBrief = symptomsBrief;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }

    // Getters
    public Long getId() { return id; }
    public Long getVisitId() { return visitId; }
    public BigDecimal getTemperatureCelsius() { return temperatureCelsius; }
    public Integer getPulseRate() { return pulseRate; }
    public String getBloodPressure() { return bloodPressure; }
    public String getSymptomsBrief() { return symptomsBrief; }
    public String getRecordedBy() { return recordedBy; }
    public OffsetDateTime getRecordedAt() { return recordedAt; }
}
