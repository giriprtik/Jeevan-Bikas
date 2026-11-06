package com.hospital.lab.service;

import com.hospital.lab.dto.LabResultResponse;
import com.hospital.lab.dto.LabResultSubmissionRequest;
import com.hospital.lab.dto.OrderResultsRequest;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.model.LabTestParameter;
import com.hospital.lab.model.LabSampleCollection;
import com.hospital.lab.repository.LabOrderRepository;
import com.hospital.lab.repository.LabSampleCollectionRepository;
import com.hospital.lab.repository.LabTestParameterRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LabResultEntryService {

    private final LabOrderRepository orderRepository;
    private final LabTestParameterRepository parameterRepository;
    private final LabResultService labResultService;
    private final LabSampleCollectionRepository sampleRepository;

    public LabResultEntryService(LabOrderRepository orderRepository,
                                 LabTestParameterRepository parameterRepository,
                                 LabResultService labResultService,
                                 LabSampleCollectionRepository sampleRepository) {
        this.orderRepository = orderRepository;
        this.parameterRepository = parameterRepository;
        this.labResultService = labResultService;
        this.sampleRepository = sampleRepository;
    }

    @Transactional
    public List<LabResultResponse> submitOrderResults(Long orderId, OrderResultsRequest request, String technician) {
        LabOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lab order not found: " + orderId));

        if (order.getOrderStatus() == LabOrderStatus.COMPLETED || order.getOrderStatus() == LabOrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is already " + order.getOrderStatus());
        }

        Set<String> orderedTests = order.getTestCodes();

        // Results can only be released once every ordered test has its sample.
        Set<String> collected = sampleRepository.findByOrderId(orderId).stream()
                .map(LabSampleCollection::getTestCode)
                .collect(Collectors.toSet());
        List<String> pending = orderedTests.stream().filter(c -> !collected.contains(c)).toList();
        if (!pending.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Collect the sample for " + String.join(", ", pending) + " before entering results");
        }
        Map<String, LabTestParameter> catalogue = parameterRepository.findByTestCodes(orderedTests).stream()
                .collect(Collectors.toMap(p -> key(p.getTest().getTestCode(), p.getParameterName()), Function.identity()));

        List<LabResultResponse> saved = new ArrayList<>();
        for (OrderResultsRequest.Entry entry : request.results()) {
            String testCode = entry.testCode().trim().toUpperCase(Locale.ROOT);
            if (!orderedTests.contains(testCode)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, testCode + " was not ordered on this order");
            }
            LabTestParameter def = catalogue.get(key(testCode, entry.parameterName()));
            if (def == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unknown parameter '" + entry.parameterName() + "' for " + testCode);
            }

            LabResultSubmissionRequest single = new LabResultSubmissionRequest();
            single.setOrderId(orderId);
            single.setTestCode(testCode);
            single.setParameterName(def.getParameterName());
            single.setObservedValue(entry.observedValue().trim());
            single.setReferenceRange(formatRange(def));
            single.setAbnormal(isOutOfRange(entry.observedValue(), def) || Boolean.TRUE.equals(entry.abnormal()));
            single.setTechnicianNotes(entry.technicianNotes());

            saved.add(labResultService.submitResult(single, technician));
        }

        order.setOrderStatus(LabOrderStatus.COMPLETED); // same transaction: all results + completion, or nothing
        return saved;
    }

    private static String key(String testCode, String parameterName) {
        return testCode.toUpperCase(Locale.ROOT) + "|" + parameterName.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isOutOfRange(String value, LabTestParameter def) {
        if (def.getRefLow() == null && def.getRefHigh() == null) return false;
        BigDecimal v;
        try {
            v = new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return false; // non-numeric (e.g. "<0.1"): rely on the manual flag
        }
        return (def.getRefLow() != null && v.compareTo(def.getRefLow()) < 0)
            || (def.getRefHigh() != null && v.compareTo(def.getRefHigh()) > 0);
    }

    private static String formatRange(LabTestParameter def) {
        String lo = def.getRefLow() == null ? null : def.getRefLow().stripTrailingZeros().toPlainString();
        String hi = def.getRefHigh() == null ? null : def.getRefHigh().stripTrailingZeros().toPlainString();
        String range = lo != null && hi != null ? lo + " - " + hi
                     : lo != null ? ">= " + lo
                     : hi != null ? "<= " + hi
                     : null;
        if (range == null) return null;
        return def.getUnit() != null ? range + " " + def.getUnit() : range;
    }
}