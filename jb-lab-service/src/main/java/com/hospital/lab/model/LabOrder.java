package com.hospital.lab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;


@Entity
@Table(name = "lab_orders", indexes = {
    @Index(name = "idx_lab_orders_visit", columnList = "visit_id"),
    @Index(name = "idx_lab_orders_mrn", columnList = "patient_mrn"),
    @Index(name = "idx_lab_orders_status", columnList = "order_status")
})
public class LabOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "patient_mrn", nullable = false, length = 20)
    private String patientMrn;

    @Column(name = "patient_name", length = 150)
    private String patientName;

    @Column(name = "ordered_by_doctor", nullable = false, length = 50)
    private String orderedByDoctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 25)
    private LabOrderStatus orderStatus = LabOrderStatus.PLACED;

    @Column(name = "clinical_history", columnDefinition = "TEXT")
    private String clinicalHistory;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lab_order_tests", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "test_code", nullable = false, length = 20)
    private Set<String> testCodes = new LinkedHashSet<>();

    @Column(name = "ordered_at", nullable = false, updatable = false)
    private OffsetDateTime orderedAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.orderedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getPatientMrn() { return patientMrn; }
    public void setPatientMrn(String patientMrn) { this.patientMrn = patientMrn; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getOrderedByDoctor() { return orderedByDoctor; }
    public void setOrderedByDoctor(String orderedByDoctor) { this.orderedByDoctor = orderedByDoctor; }
    public LabOrderStatus getOrderStatus() { return orderStatus; }
    public void setOrderStatus(LabOrderStatus orderStatus) { this.orderStatus = orderStatus; }
    public String getClinicalHistory() { return clinicalHistory; }
    public void setClinicalHistory(String clinicalHistory) { this.clinicalHistory = clinicalHistory; }
    public Set<String> getTestCodes() { return testCodes; }
    public void setTestCodes(Set<String> testCodes) { this.testCodes = testCodes; }
    public OffsetDateTime getOrderedAt() { return orderedAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
