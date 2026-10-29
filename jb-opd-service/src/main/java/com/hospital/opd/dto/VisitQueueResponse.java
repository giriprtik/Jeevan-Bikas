package com.hospital.opd.dto;

import com.hospital.opd.model.VisitPriority;
import com.hospital.opd.model.VisitStatus;

public class VisitQueueResponse {
    private Long visitId;
    private String patientMrn;
    private String patientFullName;
    private Integer tokenNumber;
    private String chiefComplaint;
    private VisitPriority priority;
    private VisitStatus status;

    public VisitQueueResponse(Long visitId, String patientMrn, String patientFullName, 
                              Integer tokenNumber, String chiefComplaint, VisitPriority priority, VisitStatus status) {
        this.visitId = visitId;
        this.patientMrn = patientMrn;
        this.patientFullName = patientFullName;
        this.tokenNumber = tokenNumber;
        this.chiefComplaint = chiefComplaint;
        this.priority = priority;
        this.status = status;
    }

    // Getters
    public Long getVisitId() { return visitId; }
    public String getPatientMrn() { return patientMrn; }
    public String getPatientFullName() { return patientFullName; }
    public Integer getTokenNumber() { return tokenNumber; }
    public String getChiefComplaint() { return chiefComplaint; }
    public VisitPriority getPriority() { return priority; }
    public VisitStatus getStatus() { return status; }
}
