package com.hospital.opd.service;

import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.dto.VisitQueueItem;
import com.hospital.opd.dto.VisitRegistrationRequest;
import com.hospital.opd.exception.ResourceNotFoundException;
import com.hospital.opd.model.*;
import com.hospital.opd.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class VisitService {

    public static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Kathmandu");

    private final VisitRepository visitRepository;
    private final PatientRepository patientRepository;
    private final TokenCounterRepository tokenCounterRepository;
    private final VisitEventRepository visitEventRepository;
    private final DoctorIntegrationService doctorIntegrationService;
    private final LabIntegrationService labIntegrationService;

    public VisitService(VisitRepository visitRepository,
                        PatientRepository patientRepository,
                        TokenCounterRepository tokenCounterRepository,
                        VisitEventRepository visitEventRepository,
                        DoctorIntegrationService doctorIntegrationService,
                        LabIntegrationService labIntegrationService) {
        this.visitRepository = visitRepository;
        this.patientRepository = patientRepository;
        this.tokenCounterRepository = tokenCounterRepository;
        this.visitEventRepository = visitEventRepository;
        this.doctorIntegrationService = doctorIntegrationService;
        this.labIntegrationService = labIntegrationService;
    }

    /** Returning patient: open a new visit by MRN. */
    @Transactional
    public VisitQueueItem registerVisit(VisitRegistrationRequest request, String registrar) {
        Patient patient = patientRepository.findByMrn(request.patientMrn().trim())
                .orElseThrow(() -> new ResourceNotFoundException("No patient found with MRN " + request.patientMrn()));
        StaffSummary doctor = doctorIntegrationService.requireDoctor(request.assignedDoctorUsername().trim());
        Visit visit = openVisit(patient, request.chiefComplaint(), request.priority(), doctor, registrar);
        return queueItem(visit.getId());
    }

    /**
     * Opens a visit and issues today's next token. Must run inside the caller's transaction so the
     * token, visit row and audit event commit (or roll back) together.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Visit openVisit(Patient patient, String chiefComplaint, VisitPriority priority,
                           StaffSummary doctor, String registrar) {
        if (patient.getId() != null && visitRepository.hasActiveVisit(patient.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    patient.getFullName() + " already has an open visit today. Complete or cancel it first.");
        }

        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        tokenCounterRepository.incrementAtomicToken(today);   // INSERT ... ON CONFLICT DO UPDATE: row-locked, race-free
        Integer token = tokenCounterRepository.getCurrentTokenValue(today);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitDate(today);
        visit.setTokenNumber(token);
        visit.setChiefComplaint(chiefComplaint.trim());
        visit.setPriority(priority);
        visit.setStatus(VisitStatus.REGISTERED);
        visit.setAssignedDoctorId(doctor.id());
        visit.setAssignedDoctorUsername(doctor.username());
        visit.setAssignedDoctorName(doctor.fullName());
        visit.setRegisteredBy(registrar);
        Visit saved = visitRepository.save(visit);

        recordEvent(saved.getId(), null, VisitStatus.REGISTERED, registrar,
                "Registered with token #" + token + ", assigned to " + doctor.fullName());
        return saved;
    }

    /** State change + audit event in one transaction. */
    @Transactional
    public void transitionStatus(Long visitId, Set<VisitStatus> allowedFrom, VisitStatus target,
                                 String actor, String note) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
        VisitStatus from = visit.getStatus();
        if (from == target) return;
        if (!allowedFrom.contains(from)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Visit is " + from + "; cannot move to " + target);
        }
        visit.setStatus(target);
        recordEvent(visitId, from, target, actor, note);
    }

    @Transactional(readOnly = true)
    public List<VisitQueueItem> getDoctorQueue(String doctorUsername, Collection<VisitStatus> statuses) {
        return visitRepository.findDoctorQueue(doctorUsername, statuses.stream().map(Enum::name).toList());
    }

    /** Order the tests first; only flip the visit to AWAITING_LAB once the lab has accepted the order. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // never hold a DB transaction across the HTTP call
    public void sendToLab(Long visitId, List<String> testCodes, String clinicalNotes,
                          String doctor, String authorization) {
        Visit visit = visitRepository.findWithPatient(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
        requireAssignedDoctor(visit, doctor);

        Set<VisitStatus> allowed = EnumSet.of(VisitStatus.TRIAGED, VisitStatus.IN_CONSULTATION);
        if (!allowed.contains(visit.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Visit is " + visit.getStatus() + "; cannot send to lab");
        }

        String history = clinicalNotes != null && !clinicalNotes.isBlank() ? clinicalNotes.trim() : visit.getChiefComplaint();
        Long labOrderId = labIntegrationService.raiseLabInvestigationOrder(
                visitId, visit.getPatient().getMrn(), visit.getPatient().getFullName(), testCodes, history, authorization);

        int updated = visitRepository.markSentToLab(visitId, allowed, VisitStatus.AWAITING_LAB, labOrderId);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lab order created but the visit changed concurrently");
        }
        recordEvent(visitId, visit.getStatus(), VisitStatus.AWAITING_LAB, doctor,
                "Lab order #" + labOrderId + ": " + String.join(", ", testCodes));
    }

    @Transactional(readOnly = true)
    public List<VisitQueueItem> getTriageQueue() {
        return visitRepository.findTriageQueue();
    }

    @Transactional(readOnly = true)
    public List<VisitQueueItem> getTodaysVisits() {
        return visitRepository.findTodaysVisits();
    }

    @Transactional(readOnly = true)
    public VisitQueueItem queueItem(Long visitId) {
        return visitRepository.findQueueItem(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
    }

    @Transactional(readOnly = true)
    public List<VisitEvent> history(Long visitId) {
        return visitEventRepository.findByVisitIdOrderByOccurredAtAsc(visitId);
    }

    public void recordEvent(Long visitId, VisitStatus from, VisitStatus to, String actor, String note) {
        visitEventRepository.save(new VisitEvent(visitId, from == null ? null : from.name(), to.name(), actor, note));
    }

    /** Doctors may only act on their own patients. */
    public static void requireAssignedDoctor(Visit visit, String doctor) {
        if (visit.getAssignedDoctorUsername() != null && !visit.getAssignedDoctorUsername().equals(doctor)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This patient is assigned to another doctor");
        }
    }
}
