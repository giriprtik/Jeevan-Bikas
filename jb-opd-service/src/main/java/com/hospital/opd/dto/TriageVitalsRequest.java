package com.hospital.opd.dto;

public class TriageVitalsRequest {
    private String appointmentId;
    private String bloodPressure;     // e.g., "120/80"
    private Double temperatureCelsius; // e.g., 37.2
    private Integer pulseRate;         // e.g., 78
    private String symptomsBrief;      // Urgent comments recorded by the nurse

    public TriageVitalsRequest() {}

    // Explicit Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public Double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(Double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public Integer getPulseRate() { return pulseRate; }
    public void setPulseRate(Integer pulseRate) { this.pulseRate = pulseRate; }

    public String getSymptomsBrief() { return symptomsBrief; }
    public void setSymptomsBrief(String symptomsBrief) { this.symptomsBrief = symptomsBrief; }
}
