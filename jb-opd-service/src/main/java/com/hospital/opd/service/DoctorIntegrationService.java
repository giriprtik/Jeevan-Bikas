package com.hospital.opd.service;

import com.hospital.opd.dto.StaffSummary;
import com.hospital.opd.exception.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/** Looks up staff in the auth service over the internal, secret-protected endpoint. */
@Service
public class DoctorIntegrationService {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient internalAuthWebClient;

    public DoctorIntegrationService(WebClient internalAuthWebClient) {
        this.internalAuthWebClient = internalAuthWebClient;
    }

    /** Returns the doctor's profile, or 400 if the username is not an active doctor. */
    public StaffSummary requireDoctor(String username) {
        StaffSummary staff;
        try {
            staff = internalAuthWebClient.get()
                    .uri(b -> b.path("/api/v1/auth/users/resolve").queryParam("username", username).build())
                    .retrieve()
                    .bodyToMono(StaffSummary.class)
                    .block(TIMEOUT);
        } catch (WebClientResponseException.NotFound ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown doctor: " + username);
        } catch (WebClientResponseException ex) {
            throw new DownstreamServiceException("auth-service", ex.getStatusCode().value(),
                    HttpStatus.BAD_GATEWAY, "Auth service returned " + ex.getStatusCode().value(), ex);
        } catch (WebClientRequestException | IllegalStateException ex) {
            throw new DownstreamServiceException("auth-service", null,
                    HttpStatus.SERVICE_UNAVAILABLE, "Auth service unreachable", ex);
        }
        if (staff == null || !"DOCTOR".equals(staff.role())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, username + " is not a doctor");
        }
        return staff;
    }
}
