package com.hospital.opd.controller;

import com.hospital.opd.dto.AppointmentRequest;
import com.hospital.opd.dto.AppointmentResponse;
import com.hospital.opd.service.OpdAppointmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opd/nurse/appointments") // Consolidated endpoint base route
@PreAuthorize("hasAnyAuthority('ROLE_NURSE', 'ROLE_ADMIN')")
public class OpdAppointmentController {

    private final OpdAppointmentService appointmentService;

    public OpdAppointmentController(OpdAppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    /**
     * NURSE ENFORCE WALK-IN APPOINTMENT CREATION
     * POST http://localhost:8080/api/v1/opd/nurse/appointments
     */
    @PostMapping("")
    public ResponseEntity<AppointmentResponse> createWalkInAppointment(
            @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal String nurseUsername) {

        // Delegate scheduling execution down to the service control layer
        AppointmentResponse payload = appointmentService.createAppointment(request, nurseUsername);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(payload);
    }
}
