package com.hospital.opd.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VisitConclusionRequest {

    @NotBlank(message = "Consultation treatment summary notes are required")
    private String consultationNotes;

    @NotBlank(message = "Final clinical assessment diagnosis statement is required")
    @Size(max = 255, message = "Diagnosis description cannot exceed 255 characters")
    private String diagnosis;

    // Getters and Setters
    public String getConsultationNotes() { return consultationNotes; }
    public void setConsultationNotes(String consultationNotes) { this.consultationNotes = consultationNotes; }
    public String getDiagnosis() { return diagnosis; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
}
