package com.hospital.lab.dto;

import java.math.BigDecimal;

public record LabTestResponse(String testCode, String testName, String department, BigDecimal cost) {}
