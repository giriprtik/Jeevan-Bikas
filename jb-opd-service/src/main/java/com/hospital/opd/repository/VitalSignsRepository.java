package com.hospital.opd.repository;

import com.hospital.opd.model.VitalSigns;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VitalSignsRepository extends JpaRepository<VitalSigns, Long> {

    /**
     * Finds the latest recorded vital signs for a specific visit encounter.
     * Essential for doctors checking the current patient profile state panel.
     */
    Optional<VitalSigns> findFirstByVisitIdOrderByRecordedAtDesc(Long visitId);

    /**
     * Returns the full history of vital adjustments over a prolonged tracking sequence.
     */
    List<VitalSigns> findByVisitIdOrderByRecordedAtDesc(Long visitId);
}
