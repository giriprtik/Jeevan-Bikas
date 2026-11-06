package com.hospital.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.auth.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static com.hospital.auth.support.TestJwt.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The auth service over HTTP on a real PostgreSQL, with the demo accounts seeded on startup
 * (admin, nurse, doctor, drkarki, labtech; password Demo@1234).
 */
class AuthApiIT extends PostgresIntegrationTest {

    private static final String DEMO_PASSWORD = "Demo@1234";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ---------------------------------------------------------------- login

    @Test
    void aSeededUserCanSignInAndUseTheToken() throws Exception {
        String token = login("doctor", DEMO_PASSWORD);

        mvc.perform(get("/api/v1/auth/profile").header(AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("doctor"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.fullName").value("Dr. Ramesh Sharma"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void aWrongPasswordIs401WithAGenericMessage() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"doctor\", \"password\": \"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid username or password."));
    }

    @Test
    void blankCredentialsAre400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"\", \"password\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- staff management

    @Test
    void onlyAnAdminCanCreateAccounts() throws Exception {
        mvc.perform(post("/api/v1/auth/register").header(AUTHORIZATION, bearer("nurse", "NURSE"))
                        .contentType(MediaType.APPLICATION_JSON).content(newUser("sneaky")))
                .andExpect(status().isForbidden());

        String admin = "Bearer " + login("admin", DEMO_PASSWORD);
        mvc.perform(post("/api/v1/auth/register").header(AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(newUser("newnurse")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newnurse"))
                .andExpect(jsonPath("$.role").value("NURSE"));

        // The new account works straight away, and the username can't be reused.
        login("newnurse", "Welcome-123");
        mvc.perform(post("/api/v1/auth/register").header(AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(newUser("newnurse")))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationValidatesTheRole() throws Exception {
        String body = newUser("badrole").replace("\"NURSE\"", "\"SURGEON\"");
        mvc.perform(post("/api/v1/auth/register").header(AUTHORIZATION, bearer("admin", "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(containsString("role")));
    }

    @Test
    void theDoctorPickerListsOnlyDoctors() throws Exception {
        mvc.perform(get("/api/v1/auth/doctors").header(AUTHORIZATION, bearer("nurse", "NURSE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username").value(hasItems("doctor", "drkarki")))
                .andExpect(jsonPath("$[*].role").value(everyItem(is("DOCTOR"))));
    }

    // ---------------------------------------------------------------- internal lookups (OPD -> auth)

    @Test
    void theSharedSecretGrantsTheLookup() throws Exception {
        mvc.perform(get("/api/v1/auth/users/resolve").param("username", "drkarki")
                        .header("X-Internal-Secret", INTERNAL_SECRET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.specialization").value("Cardiology"));
    }

    @Test
    void aWrongSecretIs401() throws Exception {
        mvc.perform(get("/api/v1/auth/users/resolve").param("username", "drkarki")
                        .header("X-Internal-Secret", "guess"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void evenAnAdminTokenCannotUseTheInternalEndpoint() throws Exception {
        mvc.perform(get("/api/v1/auth/users/resolve").param("username", "drkarki")
                        .header(AUTHORIZATION, bearer("admin", "ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownUserIs404() throws Exception {
        mvc.perform(get("/api/v1/auth/users/resolve").param("username", "ghost")
                        .header("X-Internal-Secret", INTERNAL_SECRET))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- token checks

    @Test
    void noTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/auth/doctors")).andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenSignedWithAnotherKeyIs401() throws Exception {
        mvc.perform(get("/api/v1/auth/doctors").header(AUTHORIZATION, forged("nurse", "NURSE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aValidlySignedTokenForAnAccountThatDoesNotExistIs401() throws Exception {
        mvc.perform(get("/api/v1/auth/doctors").header(AUTHORIZATION, bearer("ghost", "ADMIN")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- helpers

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    private static String newUser(String username) {
        return """
                {"username": "%s", "email": "%s@jeevanbikas.local", "password": "Welcome-123",
                 "role": "NURSE", "fullName": "New Staff", "department": "GENERAL_OPD"}
                """.formatted(username, username);
    }
}
