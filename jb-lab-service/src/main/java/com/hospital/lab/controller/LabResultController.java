package com.hospital.lab.controller;

import com.hospital.lab.dto.LabResultSubmissionRequest;
import com.hospital.lab.dto.LabResultResponse;
import com.hospital.lab.service.LabResultService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lab/results")
public class LabResultController {

    private final LabResultService labResultService;

    public LabResultController(LabResultService labResultService) {
        this.labResultService = labResultService;
    }

    /**
     * Input diagnostic measurement metrics for a specific patient lab order.
     * Restricted strictly to authorized Lab Technicians.
     */
    @PostMapping
    @PreAuthorize("hasRole('LABTECH')")
    public ResponseEntity<LabResultResponse> inputResultValues(
            @Valid @RequestBody LabResultSubmissionRequest request,
            @AuthenticationPrincipal String technicianUsername) {
        
        String actor = technicianUsername != null ? technicianUsername : "LAB_TECH_ACTOR";
        LabResultResponse response = labResultService.submitResult(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieve the documented test parameters for a given order encounter.
     * Accessible by Doctors (to review findings) and Technicians.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<List<LabResultResponse>> viewOrderResults(@PathVariable Long orderId) {
        List<LabResultResponse> response = labResultService.getResultsByOrder(orderId);
        return ResponseEntity.ok(response);
    }
}
