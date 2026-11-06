package com.hospital.lab.controller;

import com.hospital.lab.dto.LabResultResponse;
import com.hospital.lab.service.LabResultService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read access to lab results.
 * Results are written via POST /api/v1/lab/orders/{id}/results (LabOrderController),
 * which saves every parameter and completes the order in one transaction.
 */
@RestController
@RequestMapping("/api/v1/lab/results")
public class LabResultController {

    private final LabResultService labResultService;

    public LabResultController(LabResultService labResultService) {
        this.labResultService = labResultService;
    }

    /**
     * All recorded results for a lab order.
     * Doctors review findings; lab techs and admins can view what was entered.
     */
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'LABTECH', 'ADMIN')")
    public ResponseEntity<List<LabResultResponse>> viewOrderResults(@PathVariable Long orderId) {
        return ResponseEntity.ok(labResultService.getResultsByOrder(orderId));
    }
}