package com.hospital.opd.dto;

import java.math.BigDecimal;

/**
 * Read model for every worklist (triage, doctor, today's register).
 * Spring Data native-query projection: each getter maps to a column alias in VisitRepository.QUEUE_SELECT.
 */
public interface VisitQueueItem {
    Long getVisitId();
    String getPatientMrn();
    String getPatientFullName();
    Integer getTokenNumber();
    String getChiefComplaint();
    String getPriority();
    String getStatus();
    Integer getPatientAge();
    String getPatientGender();
    String getPatientBloodGroup();
    String getPatientAllergies();
    String getAssignedDoctorUsername();
    String getAssignedDoctorName();
    String getRegisteredTime();
    String getDiagnosis();

    // Latest vitals (null until triage)
    BigDecimal getTemperatureCelsius();
    Integer getPulseRate();
    String getBloodPressure();
    Integer getOxygenSaturation();
    String getSymptomsBrief();
}
