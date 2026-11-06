package com.hospital.opd.repository;

import com.hospital.opd.dto.VisitQueueItem;
import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {

    /** Shared projection for all worklists. Column aliases must match VisitQueueItem getters. */
    String QUEUE_SELECT =
        "SELECT v.id AS visitId, p.mrn AS patientMrn, p.full_name AS patientFullName, " +
        "  v.token_number AS tokenNumber, v.chief_complaint AS chiefComplaint, " +
        "  v.priority AS priority, v.status AS status, " +
        "  CAST(EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.date_of_birth)) AS INTEGER) AS patientAge, " +
        "  p.gender AS patientGender, p.blood_group AS patientBloodGroup, p.allergies AS patientAllergies, " +
        "  v.assigned_doctor_username AS assignedDoctorUsername, v.assigned_doctor_name AS assignedDoctorName, " +
        "  to_char(v.registered_at AT TIME ZONE 'Asia/Kathmandu', 'HH24:MI') AS registeredTime, " +
        "  v.diagnosis AS diagnosis, " +
        "  vs.temperature_celsius AS temperatureCelsius, vs.pulse_rate AS pulseRate, " +
        "  vs.blood_pressure AS bloodPressure, vs.oxygen_saturation AS oxygenSaturation, " +
        "  vs.symptoms_brief AS symptomsBrief " +
        "FROM visits v " +
        "JOIN patients p ON p.id = v.patient_id " +
        "LEFT JOIN LATERAL (SELECT * FROM vital_signs x WHERE x.visit_id = v.id " +
        "                   ORDER BY x.recorded_at DESC LIMIT 1) vs ON TRUE ";

    String PRIORITY_ORDER =
        "CASE v.priority WHEN 'EMERGENCY' THEN 0 WHEN 'URGENT' THEN 1 ELSE 2 END";

    String TODAY_FILTER =
        "CAST(v.registered_at AT TIME ZONE 'Asia/Kathmandu' AS DATE) = CAST(now() AT TIME ZONE 'Asia/Kathmandu' AS DATE)";

    @Query("SELECT COUNT(v) > 0 FROM Visit v WHERE v.patient.id = :patientId " +
           "AND v.status NOT IN (com.hospital.opd.model.VisitStatus.COMPLETED, com.hospital.opd.model.VisitStatus.CANCELLED)")
    boolean hasActiveVisit(@Param("patientId") Long patientId);

    /** A doctor's own worklist, emergencies first. */
    @Query(value = QUEUE_SELECT +
           "WHERE v.assigned_doctor_username = :doctor AND v.status IN (:statuses) " +
           "ORDER BY " + PRIORITY_ORDER + ", v.visit_date, v.token_number", nativeQuery = true)
    List<VisitQueueItem> findDoctorQueue(@Param("doctor") String doctorUsername,
                                         @Param("statuses") Collection<String> statuses);

    /** Triage worklist: everyone waiting for vitals, emergencies first. */
    @Query(value = QUEUE_SELECT +
           "WHERE v.status = 'REGISTERED' " +
           "ORDER BY " + PRIORITY_ORDER + ", v.visit_date, v.token_number", nativeQuery = true)
    List<VisitQueueItem> findTriageQueue();

    /** Every visit opened today (Kathmandu time), by token. */
    @Query(value = QUEUE_SELECT + "WHERE " + TODAY_FILTER + " ORDER BY v.token_number", nativeQuery = true)
    List<VisitQueueItem> findTodaysVisits();

    @Query(value = QUEUE_SELECT + "WHERE v.id = :id", nativeQuery = true)
    Optional<VisitQueueItem> findQueueItem(@Param("id") Long id);

    @Query("select v from Visit v join fetch v.patient where v.id = :id")
    Optional<Visit> findWithPatient(@Param("id") Long id);

    List<Visit> findByStatusAndLatestLabOrderIdIsNotNull(VisitStatus status);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Visit v set v.status = :to, v.latestLabOrderId = :orderId where v.id = :id and v.status in :from")
    int markSentToLab(@Param("id") Long id,
                      @Param("from") Collection<VisitStatus> from,
                      @Param("to") VisitStatus to,
                      @Param("orderId") Long orderId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("update Visit v set v.status = :to where v.id = :id and v.status in :from")
    int transition(@Param("id") Long id,
                   @Param("from") Collection<VisitStatus> from,
                   @Param("to") VisitStatus to);
}
