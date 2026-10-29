package com.hospital.opd.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "opd_appointment")
public class OpdAppointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_username", nullable = false, length = 50)
    private String patientUsername;

    @Column(name = "doctor_id", nullable = false, length = 50)
    private String doctorId;

    @Column(name = "preferred_time", nullable = false)
    private OffsetDateTime preferredTime;

    @Column(nullable = false, length = 30)
    private String status = "Scheduled";

    @Column(name = "recorded_by", length = 50)
    private String recordedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Standard Explicit Getters and Setters bypassing Lombok compiler bugs
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPatientUsername() { return patientUsername; }
    public void setPatientUsername(String patientUsername) { this.patientUsername = patientUsername; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public OffsetDateTime getPreferredTime() { return preferredTime; }
    public void setPreferredTime(OffsetDateTime preferredTime) { this.preferredTime = preferredTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
