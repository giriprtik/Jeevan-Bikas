package com.hospital.lab.service;

import com.hospital.lab.dto.LabResultSubmissionRequest;
import com.hospital.lab.dto.LabResultResponse;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.model.LabTest;
import com.hospital.lab.model.LabResult;
import com.hospital.lab.repository.LabOrderRepository;
import com.hospital.lab.repository.LabTestRepository;
import com.hospital.lab.repository.LabResultRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabResultService {

    private final LabResultRepository labResultRepository;
    private final LabOrderRepository labOrderRepository;
    private final LabTestRepository labTestRepository;

    public LabResultService(LabResultRepository labResultRepository, 
                            LabOrderRepository labOrderRepository, 
                            LabTestRepository labTestRepository) {
        this.labResultRepository = labResultRepository;
        this.labOrderRepository = labOrderRepository;
        this.labTestRepository = labTestRepository;
    }

    /**
     * Records a diagnostic observation result parameter entry.
     * Automatically transitions the parent order status to COMPLETED upon entry.
     */
    @Transactional
    public LabResultResponse submitResult(LabResultSubmissionRequest request, String technicianUsername) {
        // 1. Verify parent lab order exists
        LabOrder labOrder = labOrderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Lab order encounter record not found with ID: " + request.getOrderId()));

        // 2. Lookup test metadata parameter details from catalog
        LabTest labTest = labTestRepository.findByTestCode(request.getTestCode().toUpperCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or inactive lab test code catalog item: " + request.getTestCode()));

        // 3. Map DTO request values cleanly to database Entity structure
        LabResult result = new LabResult();
        result.setLabOrder(labOrder);
        result.setLabTest(labTest);
        result.setParameterName(request.getParameterName().trim());
        result.setObservedValue(request.getObservedValue().trim());
        
        if (request.getReferenceRange() != null) {
            result.setReferenceRange(request.getReferenceRange().trim());
        }
        
        result.setAbnormal(request.isAbnormal());
        
        if (request.getTechnicianNotes() != null) {
            result.setTechnicianNotes(request.getTechnicianNotes().trim());
        }
        
        result.setResultEnteredBy(technicianUsername);

        // 4. Persist result parameters record
        LabResult savedResult = labResultRepository.save(result);

        // 5. AUTOMATION HOOK: Advance parent order tracking state to COMPLETED automatically
        labOrder.setOrderStatus(LabOrderStatus.COMPLETED);
        labOrderRepository.save(labOrder);

        return mapToResponse(savedResult);
    }

    /**
     * Fetches all observation row parameters tied to a specific lab tracking order ID.
     */
    @Transactional(readOnly = true)
    public List<LabResultResponse> getResultsByOrder(Long orderId) {
        return labResultRepository.findByLabOrderId(orderId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LabResultResponse mapToResponse(LabResult result) {
        return new LabResultResponse(
                result.getId(),
                result.getLabOrder().getId(),
                result.getLabTest().getTestCode(),
                result.getLabTest().getTestName(),
                result.getParameterName(),
                result.getObservedValue(),
                result.getReferenceRange(),
                result.isAbnormal(),
                result.getTechnicianNotes(),
                result.getResultEnteredBy(),
                result.getResultEnteredAt()
        );
    }
}
