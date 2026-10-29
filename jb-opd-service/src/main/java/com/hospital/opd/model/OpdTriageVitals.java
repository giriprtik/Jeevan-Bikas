package com.hospital.opd.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "opd_triage_vitals")
public class OpdTriageVitals {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "appointment_id", nullable = false)
    private UUID appointmentId;

    @Column(name = "recorded_by_nurse", nullable = false, length = 50)
    private String recordedByNurse;

    @Column(name = "blood_pressure", nullable = false, length = 20)
    private String bloodPressure;

    @Column(name = "temperature_celsius", nullable = false)
    private double temperatureCelsius;

    @Column(name = "pulse_rate", nullable = false)
    private int pulseRate;

    @Column(name = "symptoms_brief", nullable = false, length = 100)
    private String symptomsBrief;


    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt = OffsetDateTime.now();

    // Standard Explicit Getters and Setters to avoid Lombok compilation bugs
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getAppointmentId() { return appointmentId; }
    public void setAppointmentId(UUID appointmentId) { this.appointmentId = appointmentId; }

    public String getRecordedByNurse() { return recordedByNurse; }
    public void setRecordedByNurse(String recordedByNurse) { this.recordedByNurse = recordedByNurse; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) { this.bloodPressure = bloodPressure; }

    public double getTemperatureCelsius() { return temperatureCelsius; }
    public void setTemperatureCelsius(double temperatureCelsius) { this.temperatureCelsius = temperatureCelsius; }

    public int getPulseRate() { return pulseRate; }
    public void setPulseRate(int pulseRate) { this.pulseRate = pulseRate; }

    public String getSymptomsBrief() { return symptomsBrief; }
    public void setSymptomsBrief(String symptomsBrief) {this.symptomsBrief = symptomsBrief; }

    public OffsetDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(OffsetDateTime recordedAt) { this.recordedAt = recordedAt; }
}
