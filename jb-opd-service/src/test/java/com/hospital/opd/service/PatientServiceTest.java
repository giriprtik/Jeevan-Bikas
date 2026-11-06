package com.hospital.opd.service;

import com.hospital.opd.dto.PatientRegisterRequest;
import com.hospital.opd.dto.PatientResponse;
import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.model.Gender;
import com.hospital.opd.model.Patient;
import com.hospital.opd.model.Visit;
import com.hospital.opd.model.VisitPriority;
import com.hospital.opd.repository.PatientRepository;
import com.hospital.opd.repository.VisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PatientServiceTest {

    private final PatientRepository patients = mock(PatientRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final VisitService visitService = mock(VisitService.class);
    private final DoctorIntegrationService doctors = mock(DoctorIntegrationService.class);
    private PatientService service;

    private static final StaffSummary DR_SHARMA =
            new StaffSummary(UUID.randomUUID(), "doctor", "Dr. Ramesh Sharma", "DOCTOR", "GENERAL_OPD", null);

    @BeforeEach
    void setUp() {
        service = new PatientService(patients, visits, visitService, doctors);
        when(patients.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(11L);
            return p;
        });
        when(patients.getNextMrnSequenceValue()).thenReturn(42L);
        when(doctors.requireDoctor("doctor")).thenReturn(DR_SHARMA);

        Visit opened = new Visit();
        opened.setId(77L);
        opened.setTokenNumber(3);
        when(visitService.openVisit(any(), any(), any(), any(), any())).thenReturn(opened);
    }

    @Test
    void createsThePatientWithASequentialMrnAndOpensTheFirstVisit() {
        PatientResponse response = service.registerPatient(request(" 9841000001 "), "nurse");

        assertThat(response.mrn()).isEqualTo("JB-000042");
        assertThat(response.id()).isEqualTo(11L);
        assertThat(response.visitId()).isEqualTo(77L);
        assertThat(response.tokenNumber()).isEqualTo(3);
        assertThat(response.phone()).isEqualTo("9841000001");

        verify(visitService).openVisit(any(Patient.class), eq("Fever for two days"), eq(VisitPriority.URGENT), eq(DR_SHARMA), eq("nurse"));
    }

    @Test
    void normalisesOptionalFields() {
        PatientRegisterRequest request = request("9841000001");
        request.setBloodGroup("o+");
        request.setAddress("   ");
        request.setAllergies(" Penicillin ");

        service.registerPatient(request, "nurse");

        ArgumentCaptor<Patient> saved = ArgumentCaptor.forClass(Patient.class);
        verify(patients).save(saved.capture());
        Patient p = saved.getValue();
        assertThat(p.getFullName()).isEqualTo("Sita Kumari");
        assertThat(p.getBloodGroup()).isEqualTo("O+");
        assertThat(p.getAddress()).isNull();
        assertThat(p.getAllergies()).isEqualTo("Penicillin");
        assertThat(p.getEmergencyContactName()).isNull();
        assertThat(p.getCreatedBy()).isEqualTo("nurse");
    }

    @Test
    void aDuplicatePhoneIsAFriendlyConflictNotAServerError() {
        Patient existing = VisitServiceTest.patient(3L, "Sita Kumari");
        when(patients.findFirstByPhone("9841000001")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerPatient(request("9841000001"), "nurse"))
                .satisfies(e -> VisitServiceTest.assertStatus(e, 409))
                .hasMessageContaining("JB-000007")
                .hasMessageContaining("Returning patient");

        verifyNoInteractions(doctors);
        verify(patients, never()).save(any());
    }

    private static PatientRegisterRequest request(String phone) {
        PatientRegisterRequest r = new PatientRegisterRequest();
        r.setFullName("  Sita Kumari ");
        r.setDateOfBirth(LocalDate.of(1992, 4, 14));
        r.setGender(Gender.FEMALE);
        r.setPhone(phone);
        r.setChiefComplaint("Fever for two days");
        r.setPriority(VisitPriority.URGENT);
        r.setAssignedDoctorUsername(" doctor ");
        return r;
    }
}
