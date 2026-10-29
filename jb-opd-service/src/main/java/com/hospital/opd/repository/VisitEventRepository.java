package com.hospital.opd.repository;

import com.hospital.opd.model.VisitEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VisitEventRepository extends JpaRepository<VisitEvent, Long> {

    /**
     * Retrieves the chronological historical record of all actions taken during a specific visit.
     * Sorted from oldest to newest to reconstruct the timeline for audits.
     */
    List<VisitEvent> findByVisitIdOrderByOccurredAtAsc(Long visitId);
}
