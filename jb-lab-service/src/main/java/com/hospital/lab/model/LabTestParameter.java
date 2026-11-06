package com.hospital.lab.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "lab_test_parameters")
public class LabTestParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private LabTest test;

    @Column(name = "parameter_name", nullable = false, length = 100)
    private String parameterName;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "ref_low", precision = 12, scale = 3)
    private BigDecimal refLow;

    @Column(name = "ref_high", precision = 12, scale = 3)
    private BigDecimal refHigh;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    public Long getId() { return id; }
    public LabTest getTest() { return test; }
    public String getParameterName() { return parameterName; }
    public String getUnit() { return unit; }
    public BigDecimal getRefLow() { return refLow; }
    public BigDecimal getRefHigh() { return refHigh; }
    public int getDisplayOrder() { return displayOrder; }
}