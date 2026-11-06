package com.hospital.lab.repository;

import com.hospital.lab.model.LabSampleCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface LabSampleCollectionRepository extends JpaRepository<LabSampleCollection, Long> {
    List<LabSampleCollection> findByOrderIdIn(Collection<Long> orderIds);
    List<LabSampleCollection> findByOrderId(Long orderId);
    boolean existsByOrderIdAndTestCode(Long orderId, String testCode);
}
