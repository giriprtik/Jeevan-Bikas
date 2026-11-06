package com.hospital.opd.controller;

import com.hospital.opd.dto.SendToLabRequest;
import com.hospital.opd.dto.VisitConclusionRequest;
import com.hospital.opd.dto.VisitLabResults;
import com.hospital.opd.dto.VisitQueueItem;
import com.hospital.opd.dto.VisitRegistrationRequest;
import com.hospital.opd.model.VisitEvent;
import com.hospital.opd.model.VisitStatus;
import com.hospital.opd.service.ConsultationWorkflowService;
import com.hospital.opd.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/opd/visits")
public class VisitController {

    private static final EnumSet<VisitStatus> DEFAULT_DOCTOR_QUEUE =
            EnumSet.of(VisitStatus.TRIAGED, VisitStatus.IN_CONSULTATION, VisitStatus.LAB_RESULTS_READY);

    private final VisitService visitService;
    private final ConsultationWorkflowService workflowService;

    public VisitController(VisitService visitService, ConsultationWorkflowService workflowService) {
        this.visitService = visitService;
        this.workflowService = workflowService;
    }

    /** Returning patient: open a new visit by MRN. */
    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<VisitQueueItem> createVisit(@Valid @RequestBody VisitRegistrationRequest request,
                                                      @AuthenticationPrincipal String registrar) {
        return new ResponseEntity<>(visitService.registerVisit(request, registrar), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN', 'LABTECH')")
    public VisitQueueItem getVisit(@PathVariable Long id) {
        return visitService.queueItem(id);
    }

    /** Audit trail: every status change with who did it and when. */
    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public List<VisitEvent> getVisitEvents(@PathVariable Long id) {
        return visitService.history(id);
    }

    /** The logged-in doctor's own worklist. Optional ?status=TRIAGED&status=AWAITING_LAB ... */
    @GetMapping("/doctor/queue")
    @PreAuthorize("hasRole('DOCTOR')")
    public List<VisitQueueItem> getMyQueue(@AuthenticationPrincipal String doctor,
                                           @RequestParam(value = "status", required = false) List<VisitStatus> status) {
        return visitService.getDoctorQueue(doctor, status == null || status.isEmpty() ? DEFAULT_DOCTOR_QUEUE : status);
    }

    /** Triage worklist: everyone waiting for vitals, emergencies first. */
    @GetMapping("/triage-queue")
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public List<VisitQueueItem> getTriageQueue() {
        return visitService.getTriageQueue();
    }

    /** Everyone registered today, any status. */
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public List<VisitQueueItem> getTodaysVisits() {
        return visitService.getTodaysVisits();
    }

    @PostMapping("/{id}/start-consultation")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> startConsultation(@PathVariable Long id, @AuthenticationPrincipal String doctor) {
        workflowService.startConsultation(id, doctor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send-to-lab")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> sendToLab(@PathVariable Long id,
                                          @Valid @RequestBody SendToLabRequest request,
                                          @AuthenticationPrincipal String doctor,
                                          @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        visitService.sendToLab(id, request.testCodes(), request.clinicalNotes(), doctor, authorization);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/conclude")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> conclude(@PathVariable Long id,
                                         @Valid @RequestBody VisitConclusionRequest request,
                                         @AuthenticationPrincipal String doctor) {
        workflowService.conclude(id, request, doctor);
        return ResponseEntity.noContent().build();
    }

    /** Pulls completed lab orders and moves their visits to LAB_RESULTS_READY. */
    @PostMapping("/lab-status/refresh")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public Map<String, Integer> refreshLabStatuses(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return Map.of("updated", workflowService.refreshLabStatuses(authorization));
    }

    /** Lab order and results for a visit. 204 if no lab order was placed. */
    @GetMapping("/{id}/lab-results")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<VisitLabResults> getLabResults(@PathVariable Long id,
                                                         @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return workflowService.getLabResults(id, authorization)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping(value = "/{id}/lab-report", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> getLabReport(@PathVariable Long id,
                                               @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        ConsultationWorkflowService.LabReport report = workflowService.getLabReport(id, authorization);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + report.fileName() + "\"")
                .body(report.content());
    }
}
