package com.hospital.opd.service;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitStatus;
import com.hospital.opd.model.VitalSigns;
import com.hospital.opd.repository.VisitRepository;
import com.hospital.opd.repository.VitalSignsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class TriageService {

    private final VitalSignsRepository vitalSignsRepository;
    private final VisitRepository visitRepository;
    private final VisitService visitService; // 👈 Coordinates the state transitions and audit logs

    public TriageService(VitalSignsRepository vitalSignsRepository, 
                         VisitRepository visitRepository, 
                         VisitService visitService) {
        this.vitalSignsRepository = vitalSignsRepository;
        this.visitRepository = visitRepository;
        this.visitService = visitService;
    }

    @Transactional
    public TriageVitalsResponse recordVitals(TriageVitalsRequest request, String nurseUsername) {
        // 1. Verify the clinical visit record exists
        Visit visit = visitRepository.findById(request.getVisitId())
                .orElseThrow(() -> new IllegalArgumentException("Active medical visit encounter not found with ID: " + request.getVisitId()));

        // 2. Perform business logic calculations (Fever / Sepsis warning checks)
        String briefNotes = request.getSymptomsBrief() != null ? request.getSymptomsBrief().trim() : "";
        if (request.getTemperatureCelsius().compareTo(new BigDecimal("37.8")) > 0) {
            briefNotes = "[HIGH FEVER ALERT] " + briefNotes;
        }

        // 3. Populate Entity object to mirror the database table structure
        VitalSigns vitals = new VitalSigns();
        vitals.setVisit(visit);
        vitals.setTemperatureCelsius(request.getTemperatureCelsius());
        vitals.setPulseRate(request.getPulseRate());
        vitals.setBloodPressure(request.getBloodPressure().trim());
        vitals.setSymptomsBrief(briefNotes);
        vitals.setRecordedBy(nurseUsername);

        // 4. Save to the vital_signs table
        VitalSigns savedVitals = vitalSignsRepository.save(vitals);

        // 5. AUTOMATION HOOK: Advance the patient's status from REGISTERED to TRIAGED and drop an audit log
        visitService.transitionStatus(visit.getId(), VisitStatus.TRIAGED, nurseUsername, "Vitals captured successfully by triage desk.");

        // 6. Return outbound response mapping
        return new TriageVitalsResponse(
                savedVitals.getId(),
                savedVitals.getVisit().getId(),
                savedVitals.getTemperatureCelsius(),
                savedVitals.getPulseRate(),
                savedVitals.getBloodPressure(),
                savedVitals.getSymptomsBrief(),
                savedVitals.getRecordedBy(),
                savedVitals.getRecordedAtReal()
        );
    }
}
