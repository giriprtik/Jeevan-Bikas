package com.hospital.opd.dto;

public class AppointmentRequest {
    private String doctorId;
    private String patientName;
    private String preferredTime;

    public AppointmentRequest() {
    }

    public AppointmentRequest(String doctorId, String preferredTime, String patientName) {
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.preferredTime = preferredTime;
    }

    // Explicit Getters and Setters bypassing Lombok
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientName() {return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }
}
