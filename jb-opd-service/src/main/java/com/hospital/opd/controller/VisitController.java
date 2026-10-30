package com.hospital.opd.controller;

import com.hospital.opd.dto.VisitRegistrationRequest;
import com.hospital.opd.dto.VisitConclusionRequest;
import com.hospital.opd.dto.VisitQueueResponse;
import com.hospital.opd.model.Visit;
import com.hospital.opd.service.VisitService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/opd/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    /**
     * Creates a new clinical encounter visit entry.
     * Restricted to frontline reception staff, nurses, or administrative desk roles.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<Visit> createEncounter(
            @Valid @RequestBody VisitRegistrationRequest request,
            @AuthenticationPrincipal String registrarUsername) {
        
        String actor = registrarUsername != null ? registrarUsername : "FRONT_DESK_ACTOR";
        Visit createdVisit = visitService.registerVisit(request, actor);
        return new ResponseEntity<>(createdVisit, HttpStatus.CREATED);
    }

    /**
     * Fetches the current daily queue list for the logged-in doctor.
     * Enforces that a doctor can only read their own queue panel workspace dashboard.
     */
    @GetMapping("/doctor/{doctorUsername}/queue")
    @PreAuthorize("hasRole('DOCTOR') and #doctorUsername == authentication.name")
    public ResponseEntity<List<VisitQueueResponse>> getMyActiveQueue(
            @PathVariable String doctorUsername) {
        
        List<Visit> rawQueue = visitService.getActiveDoctorQueue(doctorUsername);
        
        // Map elements into lightweight outbound scannable queue payloads
        List<VisitQueueResponse> responseList = rawQueue.stream()
                .map(v -> new VisitQueueResponse(
                        v.getId(),
                        v.getPatient().getMrn(),
                        v.getPatient().getFullName(),
                        v.getTokenNumber(),
                        v.getChiefComplaint(),
                        v.getPriority(),
                        v.getStatus()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    /**
     * Finalize and close out an active clinical patient visit encounter.
     * Restricted strictly to clinical Doctor roles.
     */
    @PostMapping("/{id}/conclude")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<Void> completeVisitEncounter(
            @PathVariable Long id,
            @Valid @RequestBody VisitConclusionRequest request,
            @AuthenticationPrincipal String doctorUsername) {
        
        String actor = doctorUsername != null ? doctorUsername : "ATTENDING_DOCTOR_ACTOR";
        visitService.concludeConsultation(id, request, actor);
        
        return ResponseEntity.noContent().build(); // Returns a clean 204 No Content success flag
    }

}
