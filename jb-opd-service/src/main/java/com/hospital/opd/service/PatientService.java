package com.hospital.opd.service;

import com.hospital.opd.dto.PatientRegisterRequest;
import com.hospital.opd.dto.PatientResponse;
import com.hospital.opd.model.Patient;
import com.hospital.opd.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional
    public PatientResponse registerPatient(PatientRegisterRequest request, String createdByActor) {
        // 1. Guardrail against duplicate records
        patientRepository.findByPhone(request.getPhone())
                .ifPresent(p -> {
                    throw new IllegalStateException("A patient record already exists with phone number: " + request.getPhone());
                });

        // 2. Fetch the database sequence value atomically and format the MRN string
        Long sequenceVal = patientRepository.getNextMrnSequenceValue();
        String generatedMrn = String.format("JB-%06d", sequenceVal); // Formats to 'JB-000001', 'JB-000002'...

        // 3. Map DTO fields into the database managed Entity structure
        Patient patient = new Patient();
        patient.setMrn(generatedMrn);
        patient.setFullName(request.getFullName().trim());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setDobEstimated(request.isDobEstimated());
        patient.setGender(request.getGender());
        patient.setPhone(request.getPhone().trim());
        
        if (request.getAddress() != null) patient.setAddress(request.getAddress().trim());
        if (request.getBloodGroup() != null) patient.setBloodGroup(request.getBloodGroup().toUpperCase().trim());
        if (request.getAllergies() != null) patient.setAllergies(request.getAllergies().trim());
        if (request.getEmergencyContactName() != null) patient.setEmergencyContactName(request.getEmergencyContactName().trim());
        if (request.getEmergencyContactPhone() != null) patient.setEmergencyContactPhone(request.getEmergencyContactPhone().trim());
        
        patient.setCreatedBy(createdByActor);

        // 4. Persist to database
        Patient savedPatient = patientRepository.save(patient);

        // 5. Map the saved Entity attributes cleanly into a safe outbound JSON response DTO
        return new PatientResponse(
                savedPatient.getId(),
                savedPatient.getMrn(),
                savedPatient.getFullName(),
                savedPatient.getDateOfBirth(),
                savedPatient.getGender(),
                savedPatient.getPhone()
        );
    }
}
