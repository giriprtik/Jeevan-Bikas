package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.StaffSummary;
import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.exception.InvalidCredentialsException;
import com.hospital.auth.model.UserAccount;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.support.TestJwt;
import com.hospital.auth.util.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private final UserAccountRepository users = mock(UserAccountRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4); // low cost: fast tests, same algorithm
    private final JwtTokenProvider tokens = new JwtTokenProvider(TestJwt.SECRET, 3_600_000);
    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, encoder, tokens);
        when(users.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        void issuesATokenCarryingTheUsernameRoleAndName() {
            when(users.findByUsername("doctor")).thenReturn(Optional.of(account("doctor", "DOCTOR", "Demo@1234", true)));

            LoginResponse response = service.authenticateUser(new LoginRequest(" doctor ", "Demo@1234"));

            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.role()).isEqualTo("DOCTOR");
            assertThat(response.expiresInMs()).isEqualTo(3_600_000);
            Claims claims = tokens.parse(response.accessToken());
            assertThat(claims.getSubject()).isEqualTo("doctor");
            assertThat(claims.get("roles", String.class)).isEqualTo("DOCTOR");
            assertThat(claims.get("name", String.class)).isEqualTo("Dr. doctor");
        }

        @Test
        void wrongPasswordUnknownUserAndDisabledAccountAllGetTheSameAnswer() {
            when(users.findByUsername("doctor")).thenReturn(Optional.of(account("doctor", "DOCTOR", "Demo@1234", true)));
            when(users.findByUsername("retired")).thenReturn(Optional.of(account("retired", "DOCTOR", "Demo@1234", false)));
            when(users.findByUsername("nobody")).thenReturn(Optional.empty());

            for (LoginRequest attempt : List.of(
                    new LoginRequest("doctor", "wrong-password"),
                    new LoginRequest("retired", "Demo@1234"),
                    new LoginRequest("nobody", "Demo@1234"))) {
                assertThatThrownBy(() -> service.authenticateUser(attempt))
                        .isInstanceOf(InvalidCredentialsException.class)
                        .hasMessage("Invalid username or password.");
            }
        }
    }

    @Nested
    @DisplayName("registerNewUser")
    class Register {

        @Test
        void normalisesInputAndStoresOnlyAPasswordHash() {
            UserRegisterRequest request = new UserRegisterRequest(" newdoc ", " New.Doc@Hospital.NP ", "S3cure-pass",
                    "doctor", " Dr. New Doc ", " cardiology ", " NMC-1 ", "  ");

            UserAccount saved = service.registerNewUser(request);

            assertThat(saved.getUsername()).isEqualTo("newdoc");
            assertThat(saved.getEmail()).isEqualTo("new.doc@hospital.np");
            assertThat(saved.getRole()).isEqualTo("DOCTOR");
            assertThat(saved.getFullName()).isEqualTo("Dr. New Doc");
            assertThat(saved.getDepartment()).isEqualTo("CARDIOLOGY");
            assertThat(saved.getLicenseNumber()).isEqualTo("NMC-1");
            assertThat(saved.getSpecialization()).isNull();
            assertThat(saved.isEnabled()).isTrue();
            assertThat(saved.getPassword()).isNotEqualTo("S3cure-pass").startsWith("$2");
            assertThat(encoder.matches("S3cure-pass", saved.getPassword())).isTrue();
        }

        @Test
        void aTakenUsernameIsAConflict() {
            when(users.existsByUsername("nurse")).thenReturn(true);

            assertThatThrownBy(() -> service.registerNewUser(request("nurse", "fresh@hospital.np")))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409))
                    .hasMessageContaining("Username is already taken");
            verify(users, never()).save(any());
        }

        @Test
        void aRegisteredEmailIsAConflictEvenWithDifferentCase() {
            when(users.existsByEmail("nurse@hospital.np")).thenReturn(true);

            assertThatThrownBy(() -> service.registerNewUser(request("someone", "Nurse@Hospital.NP")))
                    .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409))
                    .hasMessageContaining("Email is already registered");
        }
    }

    @Test
    void doctorPickerOnlyExposesNonSensitiveFields() {
        UserAccount doc = account("doctor", "DOCTOR", "Demo@1234", true);
        doc.setId(UUID.randomUUID());
        doc.setSpecialization("General Medicine");
        when(users.findByRoleAndEnabledTrueOrderByFullNameAsc("DOCTOR")).thenReturn(List.of(doc));

        List<StaffSummary> doctors = service.listActiveDoctors();

        assertThat(doctors).singleElement().satisfies(d -> {
            assertThat(d.id()).isEqualTo(doc.getId());
            assertThat(d.username()).isEqualTo("doctor");
            assertThat(d.specialization()).isEqualTo("General Medicine");
        });
    }

    @Test
    void resolvingADisabledUserIsNotFound() {
        when(users.findByUsername("retired")).thenReturn(Optional.of(account("retired", "DOCTOR", "x", false)));

        assertThatThrownBy(() -> service.resolveUser("retired"))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
    }

    // ---------------------------------------------------------------- helpers

    private UserAccount account(String username, String role, String rawPassword, boolean enabled) {
        UserAccount u = new UserAccount();
        u.setUsername(username);
        u.setRole(role);
        u.setFullName("Dr. " + username);
        u.setEmail(username + "@hospital.np");
        u.setPassword(encoder.encode(rawPassword));
        u.setEnabled(enabled);
        return u;
    }

    private static UserRegisterRequest request(String username, String email) {
        return new UserRegisterRequest(username, email, "S3cure-pass", "NURSE", "Some Nurse", null, null, null);
    }
}
