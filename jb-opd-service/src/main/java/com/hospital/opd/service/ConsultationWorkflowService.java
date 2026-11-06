package com.hospital.opd.service;

import com.hospital.opd.dto.LabOrderSummary;
import com.hospital.opd.dto.LabResultItem;
import com.hospital.opd.dto.VisitConclusionRequest;
import com.hospital.opd.dto.VisitLabResults;
import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitStatus;
import com.hospital.opd.repository.VisitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ConsultationWorkflowService {

    private static final Set<VisitStatus> OPENABLE = EnumSet.of(VisitStatus.TRIAGED, VisitStatus.LAB_RESULTS_READY);
    private static final Set<VisitStatus> CONCLUDABLE =
            EnumSet.of(VisitStatus.TRIAGED, VisitStatus.IN_CONSULTATION, VisitStatus.LAB_RESULTS_READY);

    private final VisitRepository visitRepository;
    private final VisitService visitService;
    private final LabIntegrationService labIntegrationService;
    private final LabReportPdfService labReportPdfService;

    public ConsultationWorkflowService(VisitRepository visitRepository,
                                       VisitService visitService,
                                       LabIntegrationService labIntegrationService,
                                       LabReportPdfService labReportPdfService) {
        this.visitRepository = visitRepository;
        this.visitService = visitService;
        this.labIntegrationService = labIntegrationService;
        this.labReportPdfService = labReportPdfService;
    }

    /** Asks lab which waiting orders are done; moves those visits to LAB_RESULTS_READY. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public int refreshLabStatuses(String authorization) {
        List<Visit> waiting = visitRepository.findByStatusAndLatestLabOrderIdIsNotNull(VisitStatus.AWAITING_LAB);
        if (waiting.isEmpty()) return 0;

        Map<Long, LabOrderSummary> orders = labIntegrationService
                .getOrders(waiting.stream().map(Visit::getLatestLabOrderId).toList(), authorization)
                .stream()
                .collect(Collectors.toMap(LabOrderSummary::id, Function.identity()));

        int moved = 0;
        for (Visit visit : waiting) {
            LabOrderSummary order = orders.get(visit.getLatestLabOrderId());
            if (order != null && "COMPLETED".equals(order.orderStatus())) {
                int changed = visitRepository.transition(visit.getId(),
                        EnumSet.of(VisitStatus.AWAITING_LAB), VisitStatus.LAB_RESULTS_READY);
                if (changed > 0) {
                    visitService.recordEvent(visit.getId(), VisitStatus.AWAITING_LAB, VisitStatus.LAB_RESULTS_READY,
                            "lab-service", "Results released for lab order #" + order.id());
                }
                moved += changed;
            }
        }
        return moved;
    }

    /** Doctor opens the patient: TRIAGED / LAB_RESULTS_READY -> IN_CONSULTATION. Idempotent. */
    @Transactional
    public void startConsultation(Long visitId, String doctor) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found: " + visitId));
        VisitService.requireAssignedDoctor(visit, doctor);

        VisitStatus from = visit.getStatus();
        if (from == VisitStatus.IN_CONSULTATION) return;
        if (!OPENABLE.contains(from)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Visit is " + from + "; cannot open consultation");
        }
        visit.setStatus(VisitStatus.IN_CONSULTATION);
        if (visit.getConsultationStartedAt() == null) {
            visit.setConsultationStartedAt(OffsetDateTime.now());
        }
        visitService.recordEvent(visitId, from, VisitStatus.IN_CONSULTATION, doctor,
                from == VisitStatus.LAB_RESULTS_READY ? "Doctor reviewing lab results" : "Consultation started");
    }

    /** Lab order + results for the visit, or empty if no lab order was placed. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<VisitLabResults> getLabResults(Long visitId, String authorization) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found: " + visitId));
        return loadLabResults(visit, authorization);
    }

    /** PDF lab report for the visit's latest (completed) lab order. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public LabReport getLabReport(Long visitId, String authorization) {
        Visit visit = visitRepository.findWithPatient(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found: " + visitId));

        VisitLabResults lab = loadLabResults(visit, authorization)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No lab order for this visit"));
        if (!"COMPLETED".equals(lab.orderStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Lab results are not released yet");
        }

        byte[] pdf = labReportPdfService.render(visit, visit.getPatient(), lab);
        String fileName = "lab-report-" + visit.getPatient().getMrn() + "-order-" + lab.orderId() + ".pdf";
        return new LabReport(fileName, pdf);
    }

    /** Doctor signs off the visit. The server, not the client, decides the status. */
    @Transactional
    public void conclude(Long visitId, VisitConclusionRequest request, String doctorUsername) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visit not found: " + visitId));
        VisitService.requireAssignedDoctor(visit, doctorUsername);

        if (!CONCLUDABLE.contains(visit.getStatus())) {
            String reason = visit.getStatus() == VisitStatus.AWAITING_LAB
                    ? "Lab results are still pending for this visit"
                    : "Visit is " + visit.getStatus() + "; cannot conclude";
            throw new ResponseStatusException(HttpStatus.CONFLICT, reason);
        }

        VisitStatus from = visit.getStatus();
        visit.setDiagnosis(request.getDiagnosis().trim());
        visit.setConsultationNotes(request.getConsultationNotes().trim());
        visit.setStatus(VisitStatus.COMPLETED);
        visit.setCompletedAt(OffsetDateTime.now());
        visitService.recordEvent(visitId, from, VisitStatus.COMPLETED, doctorUsername,
                "Signed off. Diagnosis: " + visit.getDiagnosis());
    }

    private Optional<VisitLabResults> loadLabResults(Visit visit, String authorization) {
        Long orderId = visit.getLatestLabOrderId();
        if (orderId == null) return Optional.empty();

        LabOrderSummary order = labIntegrationService.getOrders(List.of(orderId), authorization).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Lab order " + orderId + " not found in lab service"));

        List<LabResultItem> results = "COMPLETED".equals(order.orderStatus())
                ? labIntegrationService.getResults(orderId, authorization)
                : List.of();

        return Optional.of(new VisitLabResults(
                order.id(), order.orderStatus(), order.orderedByDoctor(),
                order.orderedAt(), order.testCodes(), results));
    }

    public record LabReport(String fileName, byte[] content) {}
}