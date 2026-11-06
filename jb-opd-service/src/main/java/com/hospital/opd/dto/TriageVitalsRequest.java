package com.hospital.opd.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TriageVitalsRequest {

    @NotNull(message = "visitId is required")
    @Positive(message = "visitId must be a valid visit ID")
    private Long visitId;

    @NotNull(message = "Temperature reading is required")
    @DecimalMin(value = "30.0", message = "Temperature must be clinically plausible")
    @DecimalMax(value = "45.0", message = "Temperature must be clinically plausible")
    private BigDecimal temperatureCelsius;

    @NotNull(message = "Pulse rate value is required")
    @Min(value = 20, message = "Pulse rate must be clinically plausible")
    @Max(value = 250, message = "Pulse rate must be clinically plausible")
    private Integer pulseRate;

    @NotBlank(message = "Blood pressure format summary is required")
    @Pattern(regexp = "^\\d{2,3}/\\d{2,3}$", message = "Blood pressure must follow standard format pattern (e.g. 120/80)")
    private String bloodPressure;

    @NotNull(message = "Oxygen Saturation value is required")
    @Min(value = 50, message = "Oxygen saturation must be clinically plausible")
    @Max(value = 100, message = "Oxygen saturation must be clinically plausible")
    private Integer oxygenSaturation;

    private String symptomsBrief;

    // Getters and Setters
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public BigDecimal getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(BigDecimal temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }
    public Integer getPulseRate() { return pulseRate; }
    public void setPulseRate(Integer pulseRate) { this.pulseRate = pulseRate; }
    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }
    public Integer getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(Integer oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }
    public String getSymptomsBrief() { return symptomsBrief; }
    public void setSymptomsBrief(String symptomsBrief) { this.symptomsBrief = symptomsBrief; }
}
