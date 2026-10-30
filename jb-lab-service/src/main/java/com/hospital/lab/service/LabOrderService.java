package com.hospital.lab.service;

import com.hospital.lab.dto.LabOrderRequest;
import com.hospital.lab.dto.LabOrderResponse;
import com.hospital.lab.model.LabOrder;
import com.hospital.lab.model.LabOrderStatus;
import com.hospital.lab.repository.LabOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LabOrderService {

    private final LabOrderRepository labOrderRepository;

    public LabOrderService(LabOrderRepository labOrderRepository) {
        this.labOrderRepository = labOrderRepository;
    }

    @Transactional
    public LabOrderResponse createOrder(LabOrderRequest request, String doctorUsername) {
        LabOrder order = new LabOrder();
        order.setVisitId(request.getVisitId());
        order.setPatientMrn(request.getPatientMrn());
        order.setOrderedByDoctor(doctorUsername);
        order.setClinicalHistory(request.getClinicalHistory());
        order.setOrderStatus(LabOrderStatus.PLACED);

        LabOrder saved = labOrderRepository.save(order);
        return mapToResponse(saved);
    }

    @Transactional
    public LabOrderResponse updateStatus(Long orderId, LabOrderStatus targetStatus) {
        LabOrder order = labOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Lab order not found with ID: " + orderId));
        
        order.setOrderStatus(targetStatus);
        LabOrder updated = labOrderRepository.save(order);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<LabOrderResponse> getOrdersByStatus(LabOrderStatus status) {
        return labOrderRepository.findByOrderStatus(status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LabOrderResponse mapToResponse(LabOrder order) {
        return new LabOrderResponse(
                order.getId(),
                order.getVisitId(),
                order.getPatientMrn(),
                order.getOrderedByDoctor(),
                order.getOrderStatus(),
                order.getClinicalHistory(),
                order.getOrderedAt()
        );
    }
}
