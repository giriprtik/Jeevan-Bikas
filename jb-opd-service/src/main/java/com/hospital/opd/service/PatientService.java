package com.hospital.opd.service;

import com.hospital.opd.dto.PatientRegisterRequest;
import com.hospital.opd.dto.PatientResponse;
import com.hospital.opd.dto.PatientSummary;
import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.model.Patient;
import com.hospital.opd.model.Visit;
import com.hospital.opd.repository.PatientRepository;
import com.hospital.opd.repository.VisitRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class PatientService {

    private static final int SEARCH_LIMIT = 20;

    private final PatientRepository patientRepository;
    private final VisitRepository visitRepository;
    private final VisitService visitService;
    private final DoctorIntegrationService doctorIntegrationService;

    public PatientService(PatientRepository patientRepository,
                          VisitRepository visitRepository,
                          VisitService visitService,
                          DoctorIntegrationService doctorIntegrationService) {
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.visitService = visitService;
        this.doctorIntegrationService = doctorIntegrationService;
    }

    /** New patient: creates the master record and opens their first visit in one transaction. */
    @Transactional
    public PatientResponse registerPatient(PatientRegisterRequest request, String registrar) {
        String phone = request.getPhone().trim();
        patientRepository.findFirstByPhone(phone).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A patient with phone " + phone + " is already registered as " + existing.getFullName()
                    + " (" + existing.getMrn() + "). Use 'Returning patient' to open a new visit.");
        });

        StaffSummary doctor = doctorIntegrationService.requireDoctor(request.getAssignedDoctorUsername().trim());

        Patient patient = new Patient();
        patient.setMrn(String.format("JB-%06d", patientRepository.getNextMrnSequenceValue()));
        patient.setFullName(request.getFullName().trim());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setDobEstimated(request.isDobEstimated());
        patient.setGender(request.getGender());
        patient.setPhone(phone);
        patient.setAddress(blankToNull(request.getAddress()));
        patient.setBloodGroup(request.getBloodGroup() == null ? null : blankToNull(request.getBloodGroup().toUpperCase(Locale.ROOT)));
        patient.setAllergies(blankToNull(request.getAllergies()));
        patient.setEmergencyContactName(blankToNull(request.getEmergencyContactName()));
        patient.setEmergencyContactPhone(blankToNull(request.getEmergencyContactPhone()));
        patient.setCreatedBy(registrar);
        Patient saved = patientRepository.save(patient);

        Visit visit = visitService.openVisit(saved, request.getChiefComplaint(), request.getPriority(), doctor, registrar);

        return new PatientResponse(saved.getId(), visit.getId(), visit.getTokenNumber(), saved.getMrn(),
                saved.getFullName(), saved.getDateOfBirth(), saved.getGender(), saved.getPhone());
    }

    @Transactional(readOnly = true)
    public List<PatientSummary> search(String query) {
        PageRequest page = PageRequest.of(0, SEARCH_LIMIT);
        List<Patient> found = (query == null || query.isBlank())
                ? patientRepository.findAllByOrderByCreatedAtDesc(page)
                : patientRepository.search(query.trim(), page);
        return found.stream()
                .map(p -> PatientSummary.of(p, visitRepository.hasActiveVisit(p.getId())))
                .toList();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
