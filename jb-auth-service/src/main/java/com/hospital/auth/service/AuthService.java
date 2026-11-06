package com.hospital.auth.service;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.StaffSummary;
import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.exception.InvalidCredentialsException;
import com.hospital.auth.model.UserAccount;
import com.hospital.auth.repository.UserAccountRepository;
import com.hospital.auth.util.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public UserAccount registerNewUser(UserRegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken.");
        }
        if (userAccountRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered.");
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole().trim().toUpperCase(Locale.ROOT));
        user.setEnabled(true);
        user.setCreatedAt(OffsetDateTime.now());
        user.setFullName(request.getFullName().trim());
        user.setDepartment(trimToNull(request.getDepartment(), true));
        user.setLicenseNumber(trimToNull(request.getLicenseNumber(), false));
        user.setSpecialization(trimToNull(request.getSpecialization(), false));
        return userAccountRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse authenticateUser(LoginRequest request) {
        // Same message for unknown user, wrong password and disabled account: don't leak which one it was.
        UserAccount user = userAccountRepository.findByUsername(request.username().trim())
                .filter(UserAccount::isEnabled)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password."));

        String token = tokenProvider.generateToken(user.getUsername(), user.getRole(), user.getFullName());
        return new LoginResponse(token, "Bearer", tokenProvider.getExpirationMs(),
                user.getUsername(), user.getFullName(), user.getRole());
    }

    @Transactional(readOnly = true)
    public List<StaffSummary> listActiveDoctors() {
        return userAccountRepository.findByRoleAndEnabledTrueOrderByFullNameAsc("DOCTOR").stream()
                .map(AuthService::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public StaffSummary resolveUser(String username) {
        return userAccountRepository.findByUsername(username)
                .filter(UserAccount::isEnabled)
                .map(AuthService::toSummary)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + username));
    }

    private static StaffSummary toSummary(UserAccount u) {
        return new StaffSummary(u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                u.getDepartment(), u.getSpecialization());
    }

    private static String trimToNull(String value, boolean upper) {
        if (value == null || value.isBlank()) return null;
        String t = value.trim();
        return upper ? t.toUpperCase(Locale.ROOT) : t;
    }
}
