package com.hospital.opd.service;

import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.dto.VisitRegistrationRequest;
import com.hospital.opd.exception.ResourceNotFoundException;
import com.hospital.opd.model.*;
import com.hospital.opd.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VisitServiceTest {

    private final VisitRepository visits = mock(VisitRepository.class);
    private final PatientRepository patients = mock(PatientRepository.class);
    private final TokenCounterRepository tokens = mock(TokenCounterRepository.class);
    private final VisitEventRepository events = mock(VisitEventRepository.class);
    private final DoctorIntegrationService doctors = mock(DoctorIntegrationService.class);
    private final LabIntegrationService lab = mock(LabIntegrationService.class);

    private VisitService service;

    private static final StaffSummary DR_SHARMA =
            new StaffSummary(UUID.randomUUID(), "doctor", "Dr. Ramesh Sharma", "DOCTOR", "GENERAL_OPD", "General Medicine");

    @BeforeEach
    void setUp() {
        service = new VisitService(visits, patients, tokens, events, doctors, lab);
        when(visits.save(any(Visit.class))).thenAnswer(inv -> {
            Visit v = inv.getArgument(0);
            if (v.getId() == null) v.setId(42L);
            return v;
        });
    }

    @Nested
    @DisplayName("openVisit")
    class OpenVisit {

        @Test
        void issuesTodaysNextTokenAndRecordsTheRegistrationEvent() {
            Patient patient = patient(7L, "Ram Bahadur");
            LocalDate today = LocalDate.now(VisitService.HOSPITAL_ZONE);
            when(tokens.getCurrentTokenValue(today)).thenReturn(4);

            Visit visit = service.openVisit(patient, "  Fever for 3 days ", VisitPriority.URGENT, DR_SHARMA, "nurse");

            verify(tokens).incrementAtomicToken(today);
            assertThat(visit.getTokenNumber()).isEqualTo(4);
            assertThat(visit.getVisitDate()).isEqualTo(today);
            assertThat(visit.getStatus()).isEqualTo(VisitStatus.REGISTERED);
            assertThat(visit.getChiefComplaint()).isEqualTo("Fever for 3 days");
            assertThat(visit.getAssignedDoctorUsername()).isEqualTo("doctor");
            assertThat(visit.getAssignedDoctorName()).isEqualTo("Dr. Ramesh Sharma");
            assertThat(visit.getRegisteredBy()).isEqualTo("nurse");

            VisitEvent event = capturedEvent();
            assertThat(event.getVisitId()).isEqualTo(42L);
            assertThat(event.getFromStatus()).isNull();
            assertThat(event.getToStatus()).isEqualTo("REGISTERED");
            assertThat(event.getActor()).isEqualTo("nurse");
            assertThat(event.getNote()).contains("token #4").contains("Dr. Ramesh Sharma");
        }

        @Test
        void refusesASecondOpenVisitForTheSamePatient() {
            Patient patient = patient(7L, "Ram Bahadur");
            when(visits.hasActiveVisit(7L)).thenReturn(true);

            assertThatThrownBy(() -> service.openVisit(patient, "Cough", VisitPriority.NORMAL, DR_SHARMA, "nurse"))
                    .satisfies(e -> assertStatus(e, 409))
                    .hasMessageContaining("already has an open visit");

            verifyNoInteractions(tokens);
            verify(visits, never()).save(any());
        }

        @Test
        void skipsTheOpenVisitCheckForABrandNewPatient() {
            when(tokens.getCurrentTokenValue(any())).thenReturn(1);

            service.openVisit(patient(null, "New Patient"), "Headache", VisitPriority.NORMAL, DR_SHARMA, "nurse");

            verify(visits, never()).hasActiveVisit(any());
        }
    }

    @Nested
    @DisplayName("registerVisit (returning patient)")
    class RegisterVisit {

        @Test
        void unknownMrnIsNotFound() {
            when(patients.findByMrn("JB-999999")).thenReturn(Optional.empty());
            var request = new VisitRegistrationRequest(" JB-999999 ", "Follow-up", VisitPriority.NORMAL, "doctor");

            assertThatThrownBy(() -> service.registerVisit(request, "nurse"))
                    .isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(doctors);
        }
    }

    @Nested
    @DisplayName("transitionStatus")
    class TransitionStatus {

        @Test
        void movesToTheTargetAndAuditsTheChange() {
            Visit visit = visit(5L, VisitStatus.REGISTERED, "doctor");
            when(visits.findById(5L)).thenReturn(Optional.of(visit));

            service.transitionStatus(5L, EnumSet.of(VisitStatus.REGISTERED), VisitStatus.TRIAGED, "nurse", "Vitals recorded");

            assertThat(visit.getStatus()).isEqualTo(VisitStatus.TRIAGED);
            VisitEvent event = capturedEvent();
            assertThat(event.getFromStatus()).isEqualTo("REGISTERED");
            assertThat(event.getToStatus()).isEqualTo("TRIAGED");
            assertThat(event.getNote()).isEqualTo("Vitals recorded");
        }

        @Test
        void refusesToJumpFromAStatusThatIsNotAllowed() {
            Visit visit = visit(5L, VisitStatus.COMPLETED, "doctor");
            when(visits.findById(5L)).thenReturn(Optional.of(visit));

            assertThatThrownBy(() -> service.transitionStatus(5L, EnumSet.of(VisitStatus.REGISTERED),
                    VisitStatus.TRIAGED, "nurse", null))
                    .satisfies(e -> assertStatus(e, 409));

            assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
            verifyNoInteractions(events);
        }

        @Test
        void isANoOpWhenAlreadyInTheTargetStatus() {
            when(visits.findById(5L)).thenReturn(Optional.of(visit(5L, VisitStatus.TRIAGED, "doctor")));

            service.transitionStatus(5L, EnumSet.of(VisitStatus.REGISTERED), VisitStatus.TRIAGED, "nurse", null);

            verifyNoInteractions(events);
        }

        @Test
        void unknownVisitIsNotFound() {
            when(visits.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transitionStatus(5L, EnumSet.allOf(VisitStatus.class),
                    VisitStatus.TRIAGED, "nurse", null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("sendToLab")
    class SendToLab {

        private static final String AUTH = "Bearer doctor-token";

        @Test
        void ordersTheTestsThenMovesTheVisitToAwaitingLab() {
            Visit visit = visit(5L, VisitStatus.IN_CONSULTATION, "doctor");
            when(visits.findWithPatient(5L)).thenReturn(Optional.of(visit));
            when(lab.raiseLabInvestigationOrder(eq(5L), eq("JB-000007"), eq("Ram Bahadur"), eq(List.of("CBC", "LFT")),
                    anyString(), eq(AUTH))).thenReturn(99L);
            when(visits.markSentToLab(eq(5L), anyCollection(), eq(VisitStatus.AWAITING_LAB), eq(99L))).thenReturn(1);

            service.sendToLab(5L, List.of("CBC", "LFT"), "  r/o anaemia ", "doctor", AUTH);

            verify(lab).raiseLabInvestigationOrder(5L, "JB-000007", "Ram Bahadur", List.of("CBC", "LFT"), "r/o anaemia", AUTH);
            VisitEvent event = capturedEvent();
            assertThat(event.getFromStatus()).isEqualTo("IN_CONSULTATION");
            assertThat(event.getToStatus()).isEqualTo("AWAITING_LAB");
            assertThat(event.getNote()).isEqualTo("Lab order #99: CBC, LFT");
        }

        @Test
        void fallsBackToTheChiefComplaintWhenNoClinicalNotesAreGiven() {
            Visit visit = visit(5L, VisitStatus.TRIAGED, "doctor");
            when(visits.findWithPatient(5L)).thenReturn(Optional.of(visit));
            when(lab.raiseLabInvestigationOrder(any(), any(), any(), any(), any(), any())).thenReturn(99L);
            when(visits.markSentToLab(any(), anyCollection(), any(), any())).thenReturn(1);

            service.sendToLab(5L, List.of("CBC"), "   ", "doctor", AUTH);

            verify(lab).raiseLabInvestigationOrder(5L, "JB-000007", "Ram Bahadur", List.of("CBC"), "Fever", AUTH);
        }

        @Test
        void anotherDoctorCannotOrderTestsForThisPatient() {
            when(visits.findWithPatient(5L)).thenReturn(Optional.of(visit(5L, VisitStatus.IN_CONSULTATION, "doctor")));

            assertThatThrownBy(() -> service.sendToLab(5L, List.of("CBC"), null, "drkarki", AUTH))
                    .satisfies(e -> assertStatus(e, 403));
            verifyNoInteractions(lab);
        }

        @Test
        void cannotSendAPatientWhoHasNotBeenTriaged() {
            when(visits.findWithPatient(5L)).thenReturn(Optional.of(visit(5L, VisitStatus.REGISTERED, "doctor")));

            assertThatThrownBy(() -> service.sendToLab(5L, List.of("CBC"), null, "doctor", AUTH))
                    .satisfies(e -> assertStatus(e, 409));
            verifyNoInteractions(lab);
        }

        @Test
        void reportsAConflictWhenTheVisitChangedWhileTheLabCallWasInFlight() {
            when(visits.findWithPatient(5L)).thenReturn(Optional.of(visit(5L, VisitStatus.IN_CONSULTATION, "doctor")));
            when(lab.raiseLabInvestigationOrder(any(), any(), any(), any(), any(), any())).thenReturn(99L);
            when(visits.markSentToLab(any(), anyCollection(), any(), any())).thenReturn(0);

            assertThatThrownBy(() -> service.sendToLab(5L, List.of("CBC"), null, "doctor", AUTH))
                    .satisfies(e -> assertStatus(e, 409))
                    .hasMessageContaining("changed concurrently");
            verifyNoInteractions(events);
        }
    }

    @Test
    void requireAssignedDoctorAllowsUnassignedVisits() {
        Visit unassigned = visit(5L, VisitStatus.TRIAGED, null);
        VisitService.requireAssignedDoctor(unassigned, "anyone"); // no exception
    }

    // ---------------------------------------------------------------- helpers

    private VisitEvent capturedEvent() {
        ArgumentCaptor<VisitEvent> captor = ArgumentCaptor.forClass(VisitEvent.class);
        verify(events).save(captor.capture());
        return captor.getValue();
    }

    static void assertStatus(Throwable e, int expected) {
        assertThat(e).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) e).getStatusCode().value()).isEqualTo(expected);
    }

    static Patient patient(Long id, String name) {
        Patient p = new Patient();
        p.setId(id);
        p.setMrn("JB-000007");
        p.setFullName(name);
        return p;
    }

    static Visit visit(Long id, VisitStatus status, String doctor) {
        Visit v = new Visit();
        v.setId(id);
        v.setPatient(patient(7L, "Ram Bahadur"));
        v.setStatus(status);
        v.setChiefComplaint("Fever");
        v.setAssignedDoctorUsername(doctor);
        return v;
    }
}
