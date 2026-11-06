package com.hospital.opd.controller;

import com.hospital.opd.dto.PatientRegisterRequest;
import com.hospital.opd.dto.PatientResponse;
import com.hospital.opd.dto.PatientSummary;
import com.hospital.opd.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opd/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /** New patient + first visit. */
    @PostMapping
    @PreAuthorize("hasAnyRole('NURSE', 'ADMIN')")
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody PatientRegisterRequest request,
                                                    @AuthenticationPrincipal String registrar) {
        return new ResponseEntity<>(patientService.registerPatient(request, registrar), HttpStatus.CREATED);
    }

    /** Search by MRN, phone or name (?q=). Without q: the 20 most recently registered. */
    @GetMapping
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public List<PatientSummary> search(@RequestParam(value = "q", required = false) String q) {
        return patientService.search(q);
    }
}
