package com.hospital.opd.service;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.exception.ResourceNotFoundException;
import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitStatus;
import com.hospital.opd.model.VitalSigns;
import com.hospital.opd.repository.VisitRepository;
import com.hospital.opd.repository.VitalSignsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class TriageService {

    private static final BigDecimal FEVER = new BigDecimal("37.8");
    private static final int LOW_SPO2 = 92;

    private final VitalSignsRepository vitalSignsRepository;
    private final VisitRepository visitRepository;
    private final VisitService visitService;

    public TriageService(VitalSignsRepository vitalSignsRepository,
                         VisitRepository visitRepository,
                         VisitService visitService) {
        this.vitalSignsRepository = vitalSignsRepository;
        this.visitRepository = visitRepository;
        this.visitService = visitService;
    }

    /** Records vitals; the first set moves the visit REGISTERED -> TRIAGED. Re-takes are allowed until the doctor finishes. */
    @Transactional
    public TriageVitalsResponse recordVitals(TriageVitalsRequest request, String nurse) {
        Visit visit = visitRepository.findWithPatient(request.getVisitId())
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + request.getVisitId()));

        if (visit.getStatus() == VisitStatus.COMPLETED || visit.getStatus() == VisitStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Visit is already " + visit.getStatus());
        }

        VitalSigns vitals = new VitalSigns();
        vitals.setVisit(visit);
        vitals.setTemperatureCelsius(request.getTemperatureCelsius());
        vitals.setPulseRate(request.getPulseRate());
        vitals.setBloodPressure(request.getBloodPressure().trim());
        vitals.setOxygenSaturation(request.getOxygenSaturation());
        vitals.setSymptomsBrief(withAlerts(request));
        vitals.setRecordedBy(nurse);
        VitalSigns saved = vitalSignsRepository.save(vitals);

        if (visit.getStatus() == VisitStatus.REGISTERED) {
            visitService.transitionStatus(visit.getId(), EnumSet.of(VisitStatus.REGISTERED), VisitStatus.TRIAGED,
                    nurse, "Vitals recorded: BP " + saved.getBloodPressure() + ", T " + saved.getTemperatureCelsius()
                            + "°C, HR " + saved.getPulseRate() + ", SpO2 " + saved.getOxygenSaturation() + "%");
        }
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TriageVitalsResponse> getVitalsHistoryByVisitId(Long visitId) {
        return vitalSignsRepository.findByVisitIdOrderByRecordedAtDesc(visitId).stream()
                .map(TriageService::toResponse)
                .toList();
    }

    private static String withAlerts(TriageVitalsRequest r) {
        List<String> alerts = new ArrayList<>();
        if (r.getTemperatureCelsius().compareTo(FEVER) > 0) alerts.add("[FEVER]");
        if (r.getOxygenSaturation() < LOW_SPO2) alerts.add("[LOW SPO2]");
        String notes = r.getSymptomsBrief() == null ? "" : r.getSymptomsBrief().trim();
        String prefix = String.join(" ", alerts);
        String combined = (prefix + " " + notes).trim();
        return combined.isEmpty() ? null : combined;
    }

    private static TriageVitalsResponse toResponse(VitalSigns v) {
        return new TriageVitalsResponse(v.getId(), v.getVisit().getId(), v.getVisit().getPatient().getFullName(),
                v.getTemperatureCelsius(), v.getPulseRate(), v.getBloodPressure(), v.getOxygenSaturation(),
                v.getSymptomsBrief(), v.getRecordedBy(), v.getRecordedAt());
    }
}
