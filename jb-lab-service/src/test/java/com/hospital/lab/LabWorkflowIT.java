package com.hospital.lab;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.lab.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.atomic.AtomicLong;

import static com.hospital.lab.support.TestJwt.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The lab service end to end over HTTP on a real PostgreSQL: order -> per-test sample collection ->
 * result entry -> doctor review, plus the role rules and the database constraints added in V5.
 */
class LabWorkflowIT extends PostgresIntegrationTest {

    private static final String DOCTOR = bearer("doctor", "DOCTOR");
    private static final String LABTECH = bearer("labtech", "LABTECH");
    private static final String NURSE = bearer("nurse", "NURSE");
    private static final AtomicLong VISITS = new AtomicLong(1000);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    @DisplayName("order -> collect each sample -> enter results -> doctor reads them")
    void fullLabJourney() throws Exception {
        long orderId = placeOrder("CBC", "FBS");

        // The lab worklist shows both tests pending.
        mvc.perform(get("/api/v1/lab/orders/queue").header(AUTHORIZATION, LABTECH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)].orderStatus", orderId).value(hasItem("PLACED")))
                .andExpect(jsonPath("$[?(@.id == %d)].samples[*].collected", orderId).value(contains(false, false)));

        // One sample in: still PLACED, and results are refused.
        send(post("/api/v1/lab/orders/{id}/tests/{code}/collect", orderId, "cbc"), LABTECH, null, 200);
        mvc.perform(post("/api/v1/lab/orders/{id}/results", orderId).header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content(results()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Collect the sample for FBS before entering results"));

        // Collecting the same sample twice is a conflict, not a duplicate row.
        mvc.perform(post("/api/v1/lab/orders/{id}/tests/{code}/collect", orderId, "CBC").header(AUTHORIZATION, LABTECH))
                .andExpect(status().isConflict());

        // Last sample: the order moves to SAMPLE_COLLECTED.
        JsonNode collected = send(post("/api/v1/lab/orders/{id}/tests/{code}/collect", orderId, "FBS"), LABTECH, null, 200);
        assertThat(collected.get("orderStatus").asText()).isEqualTo("SAMPLE_COLLECTED");
        assertThat(collected.get("samples").findValuesAsText("collectedBy")).containsOnly("labtech");

        // Results: low haemoglobin is flagged, the order completes in the same transaction.
        mvc.perform(post("/api/v1/lab/orders/{id}/results", orderId).header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content(results()))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$", hasSize(2)));

        mvc.perform(get("/api/v1/lab/orders").param("ids", String.valueOf(orderId)).header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderStatus").value("COMPLETED"));

        mvc.perform(get("/api/v1/lab/results/order/{id}", orderId).header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.parameterName == 'Hemoglobin')].abnormal").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.parameterName == 'Hemoglobin')].referenceRange").value(hasItem("12 - 17.5 g/dL")))
                .andExpect(jsonPath("$[?(@.parameterName == 'Fasting Glucose')].abnormal").value(hasItem(false)));

        // A completed order is off the worklist and cannot be resulted again.
        mvc.perform(get("/api/v1/lab/orders/queue").header(AUTHORIZATION, LABTECH))
                .andExpect(jsonPath("$[?(@.id == %d)]", orderId).isEmpty());
        mvc.perform(post("/api/v1/lab/orders/{id}/results", orderId).header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content(results()))
                .andExpect(status().isConflict());
    }

    @Test
    void aTestThatIsNotOnTheOrderCannotBeCollected() throws Exception {
        long orderId = placeOrder("CBC");

        mvc.perform(post("/api/v1/lab/orders/{id}/tests/{code}/collect", orderId, "LIPID").header(AUTHORIZATION, LABTECH))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("LIPID is not on order")));
    }

    @Test
    void aPlacedOrderCanBeCancelledButNotMarkedCollectedByHand() throws Exception {
        long orderId = placeOrder("CBC");

        mvc.perform(patch("/api/v1/lab/orders/{id}/status", orderId).header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"SAMPLE_COLLECTED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/v1/lab/orders/{id}/status", orderId).header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderStatus").value("CANCELLED"));
    }

    @Test
    void theCatalogueListsTheSeededTests() throws Exception {
        mvc.perform(get("/api/v1/lab/tests").header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].testCode").value(hasItems("CBC", "LFT", "FBS", "RFT", "LIPID", "URINE")));
    }

    // ---------------------------------------------------------------- security

    @Test
    void onlyDoctorsPlaceOrders() throws Exception {
        mvc.perform(post("/api/v1/lab/orders").header(AUTHORIZATION, NURSE)
                        .contentType(MediaType.APPLICATION_JSON).content(order("CBC")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/lab/orders").header(AUTHORIZATION, LABTECH)
                        .contentType(MediaType.APPLICATION_JSON).content(order("CBC")))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyLabTechniciansCollectSamples() throws Exception {
        long orderId = placeOrder("CBC");
        mvc.perform(post("/api/v1/lab/orders/{id}/tests/{code}/collect", orderId, "CBC").header(AUTHORIZATION, DOCTOR))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutAValidTokenAre401() throws Exception {
        mvc.perform(get("/api/v1/lab/orders/queue")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/lab/orders/queue").header(AUTHORIZATION, forged("labtech", "LABTECH")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/lab/orders/queue").header(AUTHORIZATION, expired("labtech", "LABTECH")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anEmptyOrderIsRejected() throws Exception {
        mvc.perform(post("/api/v1/lab/orders").header(AUTHORIZATION, DOCTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"visitId\": 1, \"patientMrn\": \"JB-000001\", \"testCodes\": []}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- database constraints (V5)

    @Test
    void aSampleRowMustBelongToATestOnTheOrder() throws Exception {
        long orderId = placeOrder("CBC");

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO lab_sample_collections (order_id, test_code, collected_by) VALUES (?, 'LIPID', 'x')", orderId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void thereIsAtMostOneSampleRowPerTest() throws Exception {
        long orderId = placeOrder("CBC");
        jdbc.update("INSERT INTO lab_sample_collections (order_id, test_code, collected_by) VALUES (?, 'CBC', 'a')", orderId);

        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO lab_sample_collections (order_id, test_code, collected_by) VALUES (?, 'CBC', 'b')", orderId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- helpers

    private long placeOrder(String... tests) throws Exception {
        JsonNode created = send(post("/api/v1/lab/orders"), DOCTOR, order(tests), 201);
        assertThat(created.get("orderedByDoctor").asText()).isEqualTo("doctor");
        return created.get("id").asLong();
    }

    private JsonNode send(MockHttpServletRequestBuilder request, String token, String body, int expectedStatus) throws Exception {
        request.header(AUTHORIZATION, token);
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        String response = mvc.perform(request)
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isEmpty() ? json.nullNode() : json.readTree(response);
    }

    private static String order(String... tests) {
        return """
                {"visitId": %d, "patientMrn": "JB-000007", "patientName": "Ram Bahadur",
                 "testCodes": ["%s"], "clinicalHistory": "Fever for two days"}
                """.formatted(VISITS.incrementAndGet(), String.join("\", \"", tests));
    }

    private static String results() {
        return """
                {"results": [
                  {"testCode": "CBC", "parameterName": "Hemoglobin", "observedValue": "10.4"},
                  {"testCode": "FBS", "parameterName": "Fasting Glucose", "observedValue": "92"}
                ]}
                """;
    }
}
