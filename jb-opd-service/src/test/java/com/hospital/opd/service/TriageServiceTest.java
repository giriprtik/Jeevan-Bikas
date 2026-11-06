package com.hospital.opd.service;

import com.hospital.opd.dto.TriageVitalsRequest;
import com.hospital.opd.dto.TriageVitalsResponse;
import com.hospital.opd.model.VisitStatus;
import com.hospital.opd.model.VitalSigns;
import com.hospital.opd.repository.VisitRepository;
import com.hospital.opd.repository.VitalSignsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TriageServiceTest {

    private final VitalSignsRepository vitals = mock(VitalSignsRepository.class);
    private final VisitRepository visits = mock(VisitRepository.class);
    private final VisitService visitService = mock(VisitService.class);
    private TriageService service;

    @BeforeEach
    void setUp() {
        service = new TriageService(vitals, visits, visitService);
        when(vitals.save(any(VitalSigns.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstVitalsMoveTheVisitFromRegisteredToTriaged() {
        when(visits.findWithPatient(5L)).thenReturn(Optional.of(VisitServiceTest.visit(5L, VisitStatus.REGISTERED, "doctor")));

        TriageVitalsResponse response = service.recordVitals(request("36.8", 96, 98, "  mild cough "), "nurse");

        assertThat(response.getVisitId()).isEqualTo(5L);
        assertThat(response.getPatientName()).isEqualTo("Ram Bahadur");
        assertThat(response.getBloodPressure()).isEqualTo("120/80");
        assertThat(response.getRecordedBy()).isEqualTo("nurse");
        assertThat(response.getSymptomsBrief()).isEqualTo("mild cough");
        verify(visitService).transitionStatus(eq(5L), eq(EnumSet.of(VisitStatus.REGISTERED)), eq(VisitStatus.TRIAGED),
                eq("nurse"), contains("BP 120/80"));
    }

    @Test
    void retakingVitalsDoesNotChangeTheStatusAgain() {
        when(visits.findWithPatient(5L)).thenReturn(Optional.of(VisitServiceTest.visit(5L, VisitStatus.IN_CONSULTATION, "doctor")));

        service.recordVitals(request("37.0", 80, 99, null), "nurse");

        verify(vitals).save(any(VitalSigns.class));
        verify(visitService, never()).transitionStatus(any(), any(), any(), any(), any());
    }

    @Test
    void vitalsCannotBeAddedToAFinishedVisit() {
        when(visits.findWithPatient(5L)).thenReturn(Optional.of(VisitServiceTest.visit(5L, VisitStatus.COMPLETED, "doctor")));

        assertThatThrownBy(() -> service.recordVitals(request("37.0", 80, 99, null), "nurse"))
                .satisfies(e -> VisitServiceTest.assertStatus(e, 409));
        verify(vitals, never()).save(any());
    }

    @ParameterizedTest(name = "T={0} SpO2={1} -> \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "37.8 | 92 | ''",                       // exactly on both thresholds: no alert
            "37.9 | 92 | [FEVER]",
            "36.5 | 91 | [LOW SPO2]",
            "39.2 | 85 | [FEVER] [LOW SPO2]",
    })
    void flagsFeverAndLowOxygenInTheSymptomNotes(String temp, int spo2, String expected) {
        when(visits.findWithPatient(5L)).thenReturn(Optional.of(VisitServiceTest.visit(5L, VisitStatus.TRIAGED, "doctor")));

        TriageVitalsResponse response = service.recordVitals(request(temp, 80, spo2, null), "nurse");

        assertThat(response.getSymptomsBrief()).isEqualTo(expected.isEmpty() ? null : expected);
    }

    @Test
    void alertsArePrefixedToTheNursesNotes() {
        when(visits.findWithPatient(5L)).thenReturn(Optional.of(VisitServiceTest.visit(5L, VisitStatus.TRIAGED, "doctor")));

        TriageVitalsResponse response = service.recordVitals(request("38.6", 110, 97, "rigors since morning"), "nurse");

        assertThat(response.getSymptomsBrief()).isEqualTo("[FEVER] rigors since morning");
    }

    private static TriageVitalsRequest request(String temp, int pulse, int spo2, String symptoms) {
        TriageVitalsRequest r = new TriageVitalsRequest();
        r.setVisitId(5L);
        r.setTemperatureCelsius(new BigDecimal(temp));
        r.setPulseRate(pulse);
        r.setBloodPressure(" 120/80 ");
        r.setOxygenSaturation(spo2);
        r.setSymptomsBrief(symptoms);
        return r;
    }
}
