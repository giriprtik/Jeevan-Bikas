package com.hospital.lab.controller;

import com.hospital.lab.dto.LabOrderRequest;
import com.hospital.lab.dto.LabOrderResponse;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.service.LabOrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/lab/orders")
public class LabOrderController {

    private final LabOrderService labOrderService;

    public LabOrderController(LabOrderService labOrderService) {
        this.labOrderService = labOrderService;
    }

    /**
     * Request a new lab test order for an OPD patient.
     * Restricted strictly to clinical Doctors.
     */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<LabOrderResponse> placeOrder(
            @Valid @RequestBody LabOrderRequest request,
            @AuthenticationPrincipal String doctorUsername) {
        
        String actor = doctorUsername != null ? doctorUsername : "UNKNOWN_DOCTOR";
        LabOrderResponse response = labOrderService.createOrder(request, actor);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Fetch all lab orders currently under a specific lifecycle status queue.
     * Accessible by Lab Technicians and Administration staff.
     */
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<List<LabOrderResponse>> getOrderQueue(
            @RequestParam LabOrderStatus status) {
        return ResponseEntity.ok(labOrderService.getOrdersByStatus(status));
    }

    /**
     * Advance a lab order status (e.g., PLACED -> SAMPLE_COLLECTED).
     * Restricted strictly to Lab Technicians.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<LabOrderResponse> changeOrderStatus(
            @PathVariable Long id,
            @RequestParam LabOrderStatus status) {
        return ResponseEntity.ok(labOrderService.updateStatus(id, status));
    }
}
