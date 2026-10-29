package com.hospital.opd.controller;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.service.OpdTriageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/opd/nurse/triage") // Clean base mapping for nursing actions
@PreAuthorize("hasAnyRole('NURSE', 'DOCTOR')") // Class-level broad guard fallback
public class OpdNurseController { // Pure Boundary Surface focused on Triage workflows

    private final OpdTriageService triageService;

    public OpdNurseController(OpdTriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * SECURE TRIAGE ENFORCEMENT ENDPOINT
     * POST http://localhost:8080/api/v1/opd/nurse/triage
     */
    @PostMapping("")
    public ResponseEntity<Map<String, String>> recordVitals(
            @RequestBody TriageVitalsRequest request,
            @AuthenticationPrincipal String username) {

        // The Boundary passes the payload down to the Entity Control layer
        triageService.recordVitals(request, username);

        return ResponseEntity.ok(Map.of(
                "status", "Success",
                "message", "Patient vitals recorded cleanly. Status updated to: In Triage"
        ));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<TriageVitalsResponse> getVitalsByAppointment(@PathVariable java.util.UUID appointmentId) {
        
        // Hand execution over to the Entity Control layer
        TriageVitalsResponse responsePayload = triageService.fetchVitalsByAppointment(appointmentId);
        
        return ResponseEntity.ok(responsePayload);
    }

}
