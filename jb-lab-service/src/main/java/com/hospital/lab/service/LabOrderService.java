package com.hospital.lab.service;

import com.hospital.lab.dto.LabOrderRequest;
import com.hospital.lab.dto.LabOrderResponse;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.model.LabSampleCollection;
import com.hospital.lab.repository.LabOrderRepository;
import com.hospital.lab.repository.LabSampleCollectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;
    private final LabSampleCollectionRepository sampleRepository;

    public LabOrderService(LabOrderRepository labOrderRepository, LabSampleCollectionRepository sampleRepository) {
        this.labOrderRepository = labOrderRepository;
        this.sampleRepository = sampleRepository;
    }

    @Transactional
    public LabOrderResponse createOrder(LabOrderRequest request, String doctorUsername) {
        LabOrder order = new LabOrder();
        order.setVisitId(request.getVisitId());
        order.setPatientMrn(request.getPatientMrn().trim());
        order.setPatientName(request.getPatientName());
        order.setOrderedByDoctor(doctorUsername);
        order.setClinicalHistory(request.getClinicalHistory());
        order.setOrderStatus(LabOrderStatus.PLACED);
        order.setTestCodes(request.getTestCodes().stream()
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .map(code -> code.toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new)));

        LabOrder saved = labOrderRepository.save(order);
        return toResponse(saved, List.of());
    }

    /**
     * Records the sample for ONE test on the order. The order stays PLACED while any test is
     * still pending and moves to SAMPLE_COLLECTED once every ordered test has its sample.
     */
    @Transactional
    public LabOrderResponse collectSample(Long orderId, String testCode, String technician) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found: " + orderId));
        if (order.getOrderStatus() != LabOrderStatus.PLACED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order is " + order.getOrderStatus() + "; no samples are pending");
        }

        String code = testCode.trim().toUpperCase(Locale.ROOT);
        if (!order.getTestCodes().contains(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, code + " is not on order #" + orderId);
        }
        if (sampleRepository.existsByOrderIdAndTestCode(orderId, code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The " + code + " sample is already collected");
        }

        // saveAndFlush so the unique constraint fires here (as a 409) if two techs click at once.
        sampleRepository.saveAndFlush(new LabSampleCollection(orderId, code, technician));

        List<LabSampleCollection> collected = sampleRepository.findByOrderId(orderId);
        if (collected.size() >= order.getTestCodes().size()) {
            order.setOrderStatus(LabOrderStatus.SAMPLE_COLLECTED);
        }
        return toResponse(order, collected);
    }

    /** Only cancellation is a manual status change; sample collection is per test, completion is via results. */
    @Transactional
    public LabOrderResponse updateStatus(Long orderId, LabOrderStatus target) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found: " + orderId));

        LabOrderStatus from = order.getOrderStatus();
        if (target == LabOrderStatus.SAMPLE_COLLECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Collect each test's sample individually: POST /api/v1/lab/orders/{id}/tests/{testCode}/collect");
        }
        boolean allowed = target == LabOrderStatus.CANCELLED
                && (from == LabOrderStatus.PLACED || from == LabOrderStatus.SAMPLE_COLLECTED);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move order from " + from + " to " + target);
        }
        order.setOrderStatus(target);
        return toResponse(order, sampleRepository.findByOrderId(orderId));
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getOrdersByStatuses(Collection<LabOrderStatus> statuses) {
        return toResponses(labOrderRepository.findByOrderStatusInOrderByOrderedAtAsc(statuses));
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getOrdersByIds(Collection<Long> ids) {
        return toResponses(labOrderRepository.findAllById(ids));
    }

    /** Batch-loads sample rows for all orders in one query (no N+1 on the worklist). */
    private List<LabOrderResponse> toResponses(List<LabOrder> orders) {
        if (orders.isEmpty()) return List.of();
        Map<Long, List<LabSampleCollection>> byOrder = sampleRepository
                .findByOrderIdIn(orders.stream().map(LabOrder::getId).toList()).stream()
                .collect(Collectors.groupingBy(LabSampleCollection::getOrderId));
        return orders.stream()
                .map(o -> toResponse(o, byOrder.getOrDefault(o.getId(), List.of())))
                .toList();
    }

    private LabOrderResponse toResponse(LabOrder order, List<LabSampleCollection> collected) {
        Map<String, LabSampleCollection> byTest = collected.stream()
                .collect(Collectors.toMap(LabSampleCollection::getTestCode, Function.identity(), (a, b) -> a));
        List<LabOrderResponse.Sample> samples = order.getTestCodes().stream()
                .map(code -> {
                    LabSampleCollection s = byTest.get(code);
                    return s == null
                            ? new LabOrderResponse.Sample(code, false, null, null)
                            : new LabOrderResponse.Sample(code, true, s.getCollectedBy(), s.getCollectedAt());
                })
                .toList();
        return new LabOrderResponse(
                order.getId(),
                order.getVisitId(),
                order.getPatientMrn(),
                order.getPatientName(),
                order.getOrderedByDoctor(),
                order.getOrderStatus(),
                order.getClinicalHistory(),
                order.getOrderedAt(),
                List.copyOf(order.getTestCodes()),
                samples);
    }
}
