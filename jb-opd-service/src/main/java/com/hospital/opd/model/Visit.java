package com.hospital.opd.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "visits", uniqueConstraints = {
    @UniqueConstraint(name = "uq_visits_token_per_day", columnNames = {"visit_date", "token_number"})
})
public class Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false, foreignKey = @ForeignKey(name = "fk_visits_patient"))
    private Patient patient;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "token_number", nullable = false)
    private Integer tokenNumber;

    @Column(name = "chief_complaint", nullable = false, columnDefinition = "TEXT")
    private String chiefComplaint;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 15)
    private VisitPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private VisitStatus status = VisitStatus.REGISTERED;

    @Column(name = "assigned_doctor", length = 50)
    private String assignedDoctor;

    @Column(name = "assigned_doctor_name", length = 150)
    private String assignedDoctorName;

    @Column(name = "consultation_notes", columnDefinition = "TEXT")
    private String consultationNotes;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "latest_lab_order_id")
    private Long latestLabOrderId;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "registered_by", nullable = false, updatable = false, length = 50)
    private String registeredBy;

    @Column(name = "consultation_started_at")
    private OffsetDateTime consultationStartedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate visitDate) { this.visitDate = visitDate; }
    public Integer getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(Integer tokenNumber) { this.tokenNumber = tokenNumber; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public VisitPriority getPriority() { return priority; }
    public void setPriority(VisitPriority priority) { this.priority = priority; }
    public VisitStatus getStatus() { return status; }
    public void setStatus(VisitStatus status) { this.status = status; }
    public String getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(String assignedDoctor) { this.assignedDoctor = assignedDoctor; }
    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }
    public String getConsultationNotes() { return consultationNotes; }
    public void setConsultationNotes(String consultationNotes) { this.consultationNotes = consultationNotes; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public Long getLatestLabOrderId() { return latestLabOrderId; }
    public void setLatestLabOrderId(Long latestLabOrderId) { this.latestLabOrderId = latestLabOrderId; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public String getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(String registeredBy) { this.registeredBy = registeredBy; }
    public OffsetDateTime getConsultationStartedAt() { return consultationStartedAt; }
    public void setConsultationStartedAt(OffsetDateTime consultationStartedAt) { this.consultationStartedAt = consultationStartedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
}
