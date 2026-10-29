package com.hospital.opd.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "visit_events", indexes = {
    @Index(name = "idx_visit_events_visit", columnList = "visit_id, occurred_at")
})
public class VisitEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visit_id", nullable = false)
    private Long visitId;

    @Column(name = "from_status", length = 25)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 25)
    private String toStatus;

    @Column(name = "actor", nullable = false, length = 50)
    private String actor;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private OffsetDateTime occurredAt;

    public VisitEvent() {}

    public VisitEvent(Long visitId, String fromStatus, String toStatus, String actor, String note) {
        this.visitId = visitId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.actor = actor;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() {
        this.occurredAt = OffsetDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getVisitId() { return visitId; }
    public void setVisitId(Long visitId) { this.visitId = visitId; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
}
