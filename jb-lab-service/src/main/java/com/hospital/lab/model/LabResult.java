package com.hospital.lab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lab_results", indexes = {
    @Index(name = "idx_lab_results_order", columnList = "order_id")
})
public class LabResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_results_order"))
    private LabOrder labOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false, foreignKey = @ForeignKey(name = "fk_results_test"))
    private LabTest labTest;

    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;

    @Column(name = "observed_value", nullable = false, length = 50)
    private String observedValue;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "is_abnormal", nullable = false)
    private boolean abnormal = false;

    @Column(name = "technician_notes", columnDefinition = "TEXT")
    private String technicianNotes;

    @Column(name = "result_entered_by", nullable = false, length = 50)
    private String resultEnteredBy;

    @Column(name = "result_entered_at", nullable = false, updatable = false)
    private OffsetDateTime resultEnteredAt;

    @PrePersist
    protected void onCreate() {
        this.resultEnteredAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LabOrder getLabOrder() { return labOrder; }
    public void setLabOrder(LabOrder labOrder) { this.labOrder = labOrder; }
    public LabTest getLabTest() { return labTest; }
    public void setLabTest(LabTest labTest) { this.labTest = labTest; }
    public String getParameterName() { return parameterName; }
    public void setParameterName(String parameterName) { this.parameterName = parameterName; }
    public String getObservedValue() { return observedValue; }
    public void setObservedValue(String observedValue) { this.observedValue = observedValue; }
    public String getReferenceRange() { return referenceRange; }
    public void setReferenceRange(String referenceRange) { this.referenceRange = referenceRange; }
    public boolean isAbnormal() { return abnormal; }
    public void setAbnormal(boolean abnormal) { this.abnormal = abnormal; }
    public String getTechnicianNotes() { return technicianNotes; }
    public void setTechnicianNotes(String technicianNotes) { this.technicianNotes = technicianNotes; }
    public String getResultEnteredBy() { return resultEnteredBy; }
    public void setResultEnteredBy(String resultEnteredBy) { this.resultEnteredBy = resultEnteredBy; }
    public OffsetDateTime getResultEnteredAt() { return resultEnteredAt; }
}
