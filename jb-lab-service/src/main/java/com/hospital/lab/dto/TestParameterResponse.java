package com.hospital.lab.dto;

import java.math.BigDecimal;

public record TestParameterResponse(
        String testCode,
        String testName,
        String parameterName,
        String unit,
        BigDecimal refLow,
        BigDecimal refHigh) {}