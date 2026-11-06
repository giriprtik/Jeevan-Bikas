package com.hospital.lab.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** A sample collected for one test on an order. Absence of a row = sample still pending. */
@Entity
@Table(name = "lab_sample_collections",
       uniqueConstraints = @UniqueConstraint(name = "uq_sample_per_test", columnNames = {"order_id", "test_code"}))
public class LabSampleCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "test_code", nullable = false, length = 20)
    private String testCode;

    @Column(name = "collected_by", nullable = false, length = 50)
    private String collectedBy;

    @Column(name = "collected_at", nullable = false, updatable = false)
    private OffsetDateTime collectedAt;

    protected LabSampleCollection() {}

    public LabSampleCollection(Long orderId, String testCode, String collectedBy) {
        this.orderId = orderId;
        this.testCode = testCode;
        this.collectedBy = collectedBy;
    }

    @PrePersist
    protected void onCreate() {
        this.collectedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getTestCode() { return testCode; }
    public String getCollectedBy() { return collectedBy; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
}
