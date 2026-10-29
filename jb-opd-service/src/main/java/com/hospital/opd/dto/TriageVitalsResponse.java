package com.hospital.opd.dto;

public class TriageVitalsResponse {
    private String vitalsRecordId;
    private String appointmentId;
    private String recordedByNurse;
    private String bloodPressure;
    private String healthSummary;

    public TriageVitalsResponse(String vitalsRecordId, String appointmentId, String recordedByNurse, String bloodPressure, String healthSummary) {
        this.vitalsRecordId = vitalsRecordId;
        this.appointmentId = appointmentId;
        this.recordedByNurse = recordedByNurse;
        this.bloodPressure = bloodPressure;
        this.healthSummary = healthSummary;
    }

    public String getVitalsRecordId() { return vitalsRecordId; }
    public String getAppointmentId() { return appointmentId; }
    public String getRecordedByNurse() { return recordedByNurse; }
    public String getBloodPressure() { return bloodPressure; }
    public String getHealthSummary() { return healthSummary; }
}
