package com.hospital.opd.service;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.model.OpdTriageVitals;
import com.hospital.opd.repository.OpdTriageVitalsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class OpdTriageService { // Active Entity Control Engine

    private final OpdTriageVitalsRepository vitalsRepository;

    public OpdTriageService(OpdTriageVitalsRepository vitalsRepository) {
        this.vitalsRepository = vitalsRepository;
    }

    @Transactional
    public TriageVitalsResponse recordVitals(TriageVitalsRequest request, String nurseUsername) {
        
        // 1. Core Data Transformation & Business Assessment
        String assessmentSummary = "Vitals Normal";
        if (request.getTemperatureCelsius() > 37.5) {
            assessmentSummary = "Fever Flagged - Alert Doctor Immediately";
        }

        // 2. Build the Model state including the new nurse symptoms comments
        OpdTriageVitals vitals = new OpdTriageVitals();
        vitals.setAppointmentId(UUID.fromString(request.getAppointmentId()));
        vitals.setRecordedByNurse(nurseUsername);
        vitals.setBloodPressure(request.getBloodPressure().trim());
        vitals.setTemperatureCelsius(request.getTemperatureCelsius());
        vitals.setPulseRate(request.getPulseRate());
        
        // FIX: Capture the brief symptoms text description safely from request DTO payload
        vitals.setSymptomsBrief(request.getSymptomsBrief() != null ? request.getSymptomsBrief().trim() : "");
        vitals.setRecordedAt(OffsetDateTime.now());

        // 3. Save to database
        OpdTriageVitals savedVitals = vitalsRepository.save(vitals);

        // 4. Return formatted Response DTO including the persisted symptoms statement
        return new TriageVitalsResponse(
                savedVitals.getId().toString(),
                savedVitals.getAppointmentId().toString(),
                savedVitals.getRecordedByNurse(),
                savedVitals.getBloodPressure(),
                savedVitals.getSymptomsBrief() // Retains direct structural parity with the model state
        );
    }

    @Transactional(readOnly = true) // Optimizes transaction performance for read-only SELECT database queries
    public TriageVitalsResponse fetchVitalsByAppointment(UUID appointmentId) {
        
        // 1. Query the repository and throw a structured exception if no database record exists
        OpdTriageVitals vitals = vitalsRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new RuntimeException("No triage vitals records found for appointment ID: " + appointmentId));

        // 2. Business Assessment Calculation mapping on the fly
        String assessmentSummary = "Vitals Normal";
        if (vitals.getTemperatureCelsius() > 37.5) {
            assessmentSummary = "Fever Flagged - Alert Doctor Immediately";
        }

        // 3. Map the retrieved Entity properties back cleanly into a safe outbound Response DTO
        return new TriageVitalsResponse(
                vitals.getId().toString(),
                vitals.getAppointmentId().toString(),
                vitals.getRecordedByNurse(),
                vitals.getBloodPressure(),
                vitals.getSymptomsBrief()        );
    }
}
