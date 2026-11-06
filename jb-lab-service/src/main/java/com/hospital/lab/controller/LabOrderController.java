package com.hospital.lab.controller;

import com.hospital.lab.dto.LabOrderRequest;
import com.hospital.lab.dto.LabOrderResponse;
import com.hospital.lab.dto.LabResultResponse;
import com.hospital.lab.dto.OrderResultsRequest;
import com.hospital.lab.dto.OrderStatusUpdateRequest;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.service.LabOrderService;
import com.hospital.lab.service.LabResultEntryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/lab/orders")
public class LabOrderController {

    /** Everything a lab tech still has to work on. */
    private static final Set<LabOrderStatus> ACTIVE_STATUSES =
            EnumSet.complementOf(EnumSet.of(LabOrderStatus.COMPLETED, LabOrderStatus.CANCELLED));

    private final LabOrderService labOrderService;
    private final LabResultEntryService resultEntryService;

    public LabOrderController(LabOrderService labOrderService, LabResultEntryService resultEntryService) {
        this.labOrderService = labOrderService;
        this.resultEntryService = resultEntryService;
    }

    /** Doctor places a lab order (called by the OPD service with the doctor's JWT). */
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<LabOrderResponse> placeOrder(
            @Valid @RequestBody LabOrderRequest request,
            @AuthenticationPrincipal String doctorUsername) {
        return new ResponseEntity<>(labOrderService.createOrder(request, doctorUsername), HttpStatus.CREATED);
    }

    /** Lab worklist. No status param = all active orders; or pass ?status=PLACED&status=SAMPLE_COLLECTED */
    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('LABTECH', 'ADMIN')")
    public ResponseEntity<List<LabOrderResponse>> getOrderQueue(
            @RequestParam(required = false) List<LabOrderStatus> status) {
        Collection<LabOrderStatus> statuses = (status == null || status.isEmpty()) ? ACTIVE_STATUSES : status;
        return ResponseEntity.ok(labOrderService.getOrdersByStatuses(statuses));
    }

    /** Lab tech collects the sample for one test on the order. */
    @PostMapping("/{id}/tests/{testCode}/collect")
    @PreAuthorize("hasRole('LABTECH')")
    public LabOrderResponse collectSample(@PathVariable Long id,
                                          @PathVariable String testCode,
                                          @AuthenticationPrincipal String technicianUsername) {
        return labOrderService.collectSample(id, testCode, technicianUsername);
    }

    /** Manual status change; only cancellation is allowed. Body: {"status":"CANCELLED"} */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('LABTECH')")
    public ResponseEntity<LabOrderResponse> changeOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(labOrderService.updateStatus(id, request.status()));
    }

    /** Lab tech submits all results for an order; the order is completed in the same transaction. */
    @PostMapping("/{id}/results")
    @PreAuthorize("hasRole('LABTECH')")
    public ResponseEntity<List<LabResultResponse>> submitOrderResults(
            @PathVariable Long id,
            @Valid @RequestBody OrderResultsRequest request,
            @AuthenticationPrincipal String technicianUsername) {
        return new ResponseEntity<>(
                resultEntryService.submitOrderResults(id, request, technicianUsername),
                HttpStatus.CREATED);
    }

    /** Look up orders by ID, used by OPD to check completion: GET /api/v1/lab/orders?ids=1,2,3 */
    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'LABTECH', 'ADMIN')")
    public ResponseEntity<List<LabOrderResponse>> getOrders(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(labOrderService.getOrdersByIds(ids));
    }
}