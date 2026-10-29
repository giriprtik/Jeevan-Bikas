package com.hospital.opd.service;

import com.hospital.opd.dto.VisitRegistrationRequest;
import com.hospital.opd.model.*;
import com.hospital.opd.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final TokenCounterRepository tokenCounterRepository;
    private final VisitEventRepository visitEventRepository;

    public VisitService(VisitRepository visitRepository,
                        PatientRepository patientRepository,
                        TokenCounterRepository tokenCounterRepository,
                        VisitEventRepository visitEventRepository) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.tokenCounterRepository = tokenCounterRepository;
        this.visitEventRepository = visitEventRepository;
    }

    /**
     * Registers a new patient outpatient clinic encounter.
     * Automates thread-safe daily token increments and creates an initial audit log entry.
     */
    @Transactional
    public Visit registerVisit(VisitRegistrationRequest request, String registrarUsername) {
        // 1. Fetch patient master context by MRN
        Patient patient = patientRepository.findByMrn(request.getPatientMrn())
                .orElseThrow(() -> new IllegalArgumentException("No registered patient profile found for MRN: " + request.getPatientMrn()));

        // 2. Guardrail Constraint Check: Assert patient doesn't already have an open visit today
        if (visitRepository.hasActiveVisit(patient.getId())) {
            throw new IllegalStateException("Patient currently has an active open visit case record. Close or cancel the prior session first.");
        }

        // 3. Evaluate contemporary Nepal date window for atomic token allocation grouping
        LocalDate todayInNepal = LocalDate.now();

        // 4. Fire thread-safe upsert routine to advance token queue numbers atomically
        tokenCounterRepository.incrementAtomicToken(todayInNepal);
        Integer assignedToken = tokenCounterRepository.getCurrentTokenValue(todayInNepal);

        // 5. Instantiation mapping configuration 
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitDate(todayInNepal);
        visit.setTokenNumber(assignedToken);
        visit.setChiefComplaint(request.getChiefComplaint().trim());
        visit.setPriority(request.getPriority());
        visit.setStatus(VisitStatus.REGISTERED);
        
        if (request.getAssignedDoctor() != null) {
            visit.setAssignedDoctor(request.getAssignedDoctor().trim());
        }
        if (request.getAssignedDoctorName() != null) {
            visit.setAssignedDoctorName(request.getAssignedDoctorName().trim());
        }
        
        visit.setRegisteredBy(registrarUsername);

        Visit savedVisit = visitRepository.save(visit);

        // 6. Write Initial Audit Stamp Entry Ledger
        VisitEvent initialLog = new VisitEvent(
                savedVisit.getId(),
                null, // fromStatus is null for fresh registrations
                VisitStatus.REGISTERED.name(),
                registrarUsername,
                "Patient successfully registered at entry desk. Issued Token #" + assignedToken
        );
        visitEventRepository.save(initialLog);

        return savedVisit;
    }

    /**
     * Central State Machine Engine.
     * Transitions visit statuses across authorized workflow boundaries and logs actions in the history ledger.
     */
    @Transactional
    public void transitionStatus(Long visitId, VisitStatus targetStatus, String actorUsername, String customLogNote) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("Encounter visit file record not found for ID: " + visitId));

        String historicalPreviousStatus = visit.getStatus().name();

        // Business Rule Checks: Capture timestamp adjustments for milestones
        if (targetStatus == VisitStatus.IN_CONSULTATION) {
            visit.setConsultationStartedAt(OffsetDateTime.now());
        } else if (targetStatus == VisitStatus.COMPLETED || targetStatus == VisitStatus.CANCELLED) {
            visit.setCompletedAt(OffsetDateTime.now());
        }

        // Update active database row
        visit.setStatus(targetStatus);
        visitRepository.save(visit);

        // Drop automated append-only audit tracking record
        VisitEvent transitionLog = new VisitEvent(
                visit.getId(),
                historicalPreviousStatus,
                targetStatus.name(),
                actorUsername,
                customLogNote
        );
        visitEventRepository.save(transitionLog);
    }

    /**
     * Reads current active queue waiting panel details for a specific doctor.
     */
    @Transactional(readOnly = true)
    public List<Visit> getActiveDoctorQueue(String doctorUsername) {
        LocalDate todayInNepal = LocalDate.now();
        // Fetches only patients who have finished triage and are waiting to see the doctor
        return visitRepository.findActiveQueue(doctorUsername, todayInNepal, VisitStatus.TRIAGED);
    }
}
