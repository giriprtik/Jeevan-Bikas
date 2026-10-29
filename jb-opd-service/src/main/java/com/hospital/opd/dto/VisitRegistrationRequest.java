package com.hospital.opd.dto;

import com.hospital.opd.model.VisitPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class VisitRegistrationRequest {

    @NotBlank(message = "Patient MRN is required")
    private String patientMrn;

    @NotBlank(message = "Chief complaint summary description is required")
    private String chiefComplaint;

    @NotNull(message = "Triage entry priority is required (NORMAL, URGENT, EMERGENCY)")
    private VisitPriority priority;

    @Size(max = 50)
    private String assignedDoctor;

    @Size(max = 150)
    private String assignedDoctorName;

    // Getters and Setters
    public String getPatientMrn() { return patientMrn; }
    public void setPatientMrn(String patientMrn) { this.patientMrn = patientMrn; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public VisitPriority getPriority() { return priority; }
    public void setPriority(VisitPriority priority) { this.priority = priority; }
    public String getAssignedDoctor() { return assignedDoctor; }
    public void setAssignedDoctor(String assignedDoctor) { this.assignedDoctor = assignedDoctor; }
    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }
}
