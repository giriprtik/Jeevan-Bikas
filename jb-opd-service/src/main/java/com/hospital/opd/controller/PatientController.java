package com.hospital.opd.controller;

import com.hospital.opd.dto.PatientRegisterRequest;
import com.hospital.opd.dto.PatientResponse;
import com.hospital.opd.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opd/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /**
     * Endpoint to register new patients. 
     * Restricted to frontline reception staff, nurses, or administrative managers.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientRegisterRequest request,
            @AuthenticationPrincipal String registrarUsername) {
        
        // Use registrarUsername if present, fallback safely if authentication context handles principal differently
        String actor = registrarUsername != null ? registrarUsername : "SYSTEM_DESK";
        
        PatientResponse response = patientService.registerPatient(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
