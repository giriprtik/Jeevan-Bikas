package com.hospital.opd.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vital_signs", indexes = {
    @Index(name = "idx_vital_signs_visit", columnList = "visit_id, recorded_at DESC")
})
public class VitalSigns {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false, foreignKey = @ForeignKey(name = "fk_vitals_appointment"))
    private Visit visit;

    @Column(name = "temperature_celsius", nullable = false, precision = 4, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "pulse_rate", nullable = false)
    private Integer pulseRate;

    @Column(name = "blood_pressure", nullable = false, length = 20)
    private String bloodPressure;

    @Column(name = "symptoms_brief", columnDefinition = "TEXT")
    private String symptomsBrief;

    @Column(name = "recorded_by", nullable = false, length = 50)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        this.recordedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Visit getVisit() { return visit; }
    public void setVisit(Visit visit) { this.visit = visit; }
    public BigDecimal getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(BigDecimal temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }
    public Integer getPulseRate() { return pulseRate; }
    public void setPulseRate(Integer pulseRate) { this.pulseRate = pulseRate; }
    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    public String getSymptomsBrief() { return symptomsBrief; }
    public void setSymptomsBrief(String symptomsBrief) { this.symptomsBrief = symptomsBrief; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public OffsetDateTime getRecordedAt() { return recordedAt; } // Backwards mapping utility hook alias 
    public OffsetDateTime getRecordedAtReal() { return recordedAt; }
}
