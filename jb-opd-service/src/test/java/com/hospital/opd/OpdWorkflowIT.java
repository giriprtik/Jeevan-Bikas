package com.hospital.opd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.repository.TokenCounterRepository;
import com.hospital.opd.service.DoctorIntegrationService;
import com.hospital.opd.service.LabIntegrationService;
import com.hospital.opd.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.hospital.opd.support.TestJwt.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The OPD service end to end over HTTP, on a real PostgreSQL: security rules, the visit state machine,
 * the native worklist queries (LATERAL join + projection) and the race-free token counter.
 * The auth and lab services are replaced by mocks; their own tests cover them.
 */
class OpdWorkflowIT extends PostgresIntegrationTest {

    private static final String NURSE = bearer("nurse", "NURSE");
    private static final String DOCTOR = bearer("doctor", "DOCTOR");
    private static final String OTHER_DOCTOR = bearer("drkarki", "DOCTOR");
    private static final String LABTECH = bearer("labtech", "LABTECH");
    private static final AtomicLong PHONES = new AtomicLong(9_841_000_000L);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TokenCounterRepository tokens;
    @Autowired PlatformTransactionManager txManager;

    @MockBean DoctorIntegrationService doctors;
    @MockBean LabIntegrationService lab;

    @BeforeEach
    void knownDoctors() {
        when(doctors.requireDoctor("doctor")).thenReturn(
                new StaffSummary(UUID.randomUUID(), "doctor", "Dr. Ramesh Sharma", "DOCTOR", "GENERAL_OPD", "General Medicine"));
        when(doctors.requireDoctor("ghost")).thenThrow(
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown doctor: ghost"));
    }

    @Test
    @DisplayName("registration -> triage -> doctor's queue -> consultation -> lab, with an audit trail")
    void fullOpdJourney() throws Exception {
        JsonNode registered = send(post("/api/v1/opd/patients"), NURSE, newPatient(nextPhone()), 201);
        long visitId = registered.get("visitId").asLong();
        assertThat(registered.get("mrn").asText()).matches("JB-\\d{6}");
        assertThat(registered.get("tokenNumber").asInt()).isPositive();

        // Nurse's triage worklist (native query) shows the new visit.
        mvc.perform(get("/api/v1/opd/visits/triage-queue").header(AUTHORIZATION, NURSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.visitId == %d)].status", visitId).value(hasItem("REGISTERED")))
                .andExpect(jsonPath("$[?(@.visitId == %d)].patientFullName", visitId).value(hasItem("Sita Kumari")));

        // Vitals move it to TRIAGED and flag the fever.
        String vitals = """
                {"visitId": %d, "temperatureCelsius": 38.4, "pulseRate": 96,
                 "bloodPressure": "120/80", "oxygenSaturation": 97, "symptomsBrief": "body ache"}
                """.formatted(visitId);
        JsonNode recorded = send(post("/api/v1/opd/triage/vitals"), NURSE, vitals, 201);
        assertThat(recorded.get("symptomsBrief").asText()).isEqualTo("[FEVER] body ache");

        // The assigned doctor sees it with the latest vitals (LEFT JOIN LATERAL); the other doctor does not.
        mvc.perform(get("/api/v1/opd/visits/doctor/queue").header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.visitId == %d)].status", visitId).value(hasItem("TRIAGED")))
                .andExpect(jsonPath("$[?(@.visitId == %d)].pulseRate", visitId).value(hasItem(96)))
                .andExpect(jsonPath("$[?(@.visitId == %d)].bloodPressure", visitId).value(hasItem("120/80")));
        mvc.perform(get("/api/v1/opd/visits/doctor/queue").header(AUTHORIZATION, OTHER_DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.visitId == %d)]", visitId).isEmpty());

        // Only the assigned doctor can open the consultation.
        mvc.perform(post("/api/v1/opd/visits/{id}/start-consultation", visitId).header(AUTHORIZATION, OTHER_DOCTOR))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/opd/visits/{id}/start-consultation", visitId).header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isNoContent());

        // Send to lab: the lab call is mocked, the visit moves to AWAITING_LAB.
        when(lab.raiseLabInvestigationOrder(eq(visitId), anyString(), eq("Sita Kumari"), eq(List.of("CBC", "LFT")),
                eq("r/o enteric fever"), eq(DOCTOR))).thenReturn(501L);
        send(post("/api/v1/opd/visits/{id}/send-to-lab", visitId), DOCTOR,
                "{\"testCodes\": [\"CBC\", \"LFT\"], \"clinicalNotes\": \"r/o enteric fever\"}", 204);
        mvc.perform(get("/api/v1/opd/visits/{id}", visitId).header(AUTHORIZATION, NURSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_LAB"));

        // Every step is in the audit trail, in order.
        mvc.perform(get("/api/v1/opd/visits/{id}/events", visitId).header(AUTHORIZATION, NURSE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].toStatus").value(contains("REGISTERED", "TRIAGED", "IN_CONSULTATION", "AWAITING_LAB")))
                .andExpect(jsonPath("$[3].note").value("Lab order #501: CBC, LFT"));
    }

    // ---------------------------------------------------------------- registration rules

    @Test
    void aPatientCannotHaveTwoOpenVisits() throws Exception {
        JsonNode registered = send(post("/api/v1/opd/patients"), NURSE, newPatient(nextPhone()), 201);
        String returning = """
                {"patientMrn": "%s", "chiefComplaint": "Same again", "priority": "NORMAL", "assignedDoctorUsername": "doctor"}
                """.formatted(registered.get("mrn").asText());

        mvc.perform(post("/api/v1/opd/visits").header(AUTHORIZATION, NURSE)
                        .contentType(MediaType.APPLICATION_JSON).content(returning))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("already has an open visit")));
    }

    @Test
    void aDuplicatePhoneNumberIsAConflictWithAHelpfulMessage() throws Exception {
        String phone = nextPhone();
        send(post("/api/v1/opd/patients"), NURSE, newPatient(phone), 201);

        mvc.perform(post("/api/v1/opd/patients").header(AUTHORIZATION, NURSE)
                        .contentType(MediaType.APPLICATION_JSON).content(newPatient(phone)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("Returning patient")));
    }

    @Test
    void missingFieldsAreA400ThatNamesThem() throws Exception {
        mvc.perform(post("/api/v1/opd/patients").header(AUTHORIZATION, NURSE)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(allOf(containsString("fullName"), containsString("phone"))));
    }

    @Test
    void anUnknownDoctorIsRejected() throws Exception {
        mvc.perform(post("/api/v1/opd/patients").header(AUTHORIZATION, NURSE)
                        .contentType(MediaType.APPLICATION_JSON).content(newPatient(nextPhone(), "ghost")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Unknown doctor: ghost"));
    }

    // ---------------------------------------------------------------- security

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/opd/visits/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void aTokenSignedWithAnotherKeyIs401() throws Exception {
        mvc.perform(get("/api/v1/opd/visits/today").header(AUTHORIZATION, forged("nurse", "NURSE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anExpiredTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/opd/visits/today").header(AUTHORIZATION, expired("nurse", "NURSE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doctorsCannotRegisterPatients() throws Exception {
        mvc.perform(post("/api/v1/opd/patients").header(AUTHORIZATION, DOCTOR)
                        .contentType(MediaType.APPLICATION_JSON).content(newPatient(nextPhone())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Your role is not allowed to do this."));
    }

    @Test
    void nursesHaveNoDoctorQueue() throws Exception {
        mvc.perform(get("/api/v1/opd/visits/doctor/queue").header(AUTHORIZATION, NURSE))
                .andExpect(status().isForbidden());
    }

    @Test
    void labTechniciansCannotSeeTheTriageQueue() throws Exception {
        mvc.perform(get("/api/v1/opd/visits/triage-queue").header(AUTHORIZATION, LABTECH))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("token numbers stay unique when many registrations hit at once")
    void tokenCounterIsRaceFree() throws Exception {
        LocalDate day = LocalDate.of(2099, 1, 1); // a day no other test touches
        TransactionTemplate tx = new TransactionTemplate(txManager);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<Integer>> issued = new ArrayList<>();
            for (int i = 0; i < 40; i++) {
                issued.add(pool.submit(() -> tx.execute(status -> {
                    tokens.incrementAtomicToken(day);
                    return tokens.getCurrentTokenValue(day);
                })));
            }
            Set<Integer> numbers = new HashSet<>();
            for (Future<Integer> f : issued) numbers.add(f.get(30, TimeUnit.SECONDS));

            assertThat(numbers).hasSize(40).contains(1, 40);
        } finally {
            pool.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- helpers

    private JsonNode send(MockHttpServletRequestBuilder request, String token, String body, int expectedStatus) throws Exception {
        String response = mvc.perform(request.header(AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.nullNode() : json.readTree(response);
    }

    private static String nextPhone() {
        return String.valueOf(PHONES.incrementAndGet());
    }

    private static String newPatient(String phone) {
        return newPatient(phone, "doctor");
    }

    private static String newPatient(String phone, String doctor) {
        return """
                {"fullName": "Sita Kumari", "dateOfBirth": "1992-04-14", "gender": "FEMALE", "phone": "%s",
                 "chiefComplaint": "Fever for two days", "priority": "URGENT", "assignedDoctorUsername": "%s"}
                """.formatted(phone, doctor);
    }
}
