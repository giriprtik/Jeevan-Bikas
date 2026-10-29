package com.hospital.opd.controller;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.service.TriageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opd/triage")
public class TriageController {

    private final TriageService triageService;

    public TriageController(TriageService triageService) {
        this.triageService = triageService;
    }

    /**
     * Endpoint for clinical triage staff to document a patient's physical vital signs.
     * Automatically transitions the patient visit state engine under the hood.
     */
    @PostMapping("/vitals")
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<TriageVitalsResponse> captureVitals(
            @Valid @RequestBody TriageVitalsRequest request,
            @AuthenticationPrincipal String nurseUsername) {
        
        String actor = nurseUsername != null ? nurseUsername : "TRIAGE_DESK_NURSE";
        TriageVitalsResponse response = triageService.recordVitals(request, actor);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
