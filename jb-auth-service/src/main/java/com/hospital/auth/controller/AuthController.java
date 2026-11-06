package com.hospital.auth.controller;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.StaffSummary;
import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.model.UserAccount;
import com.hospital.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.authenticateUser(request);
    }

    /** Staff accounts are created by an administrator; there is no public sign-up. */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegisterRequest request) {
        UserAccount created = authService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "username", created.getUsername(),
                "role", created.getRole()));
    }

    @GetMapping("/profile")
    public StaffSummary profile(@AuthenticationPrincipal UserAccount user) {
        return authService.resolveUser(user.getUsername());
    }

    /** Doctor picker for the registration desk. */
    @GetMapping("/doctors")
    @PreAuthorize("hasAnyRole('NURSE', 'DOCTOR', 'ADMIN')")
    public List<StaffSummary> doctors() {
        return authService.listActiveDoctors();
    }

    /** Internal (service-to-service, X-Internal-Secret): full staff lookup. */
    @GetMapping("/users/resolve")
    @PreAuthorize("hasRole('SYSTEM')")
    public StaffSummary resolve(@RequestParam("username") String username) {
        return authService.resolveUser(username);
    }

    /** Internal (service-to-service, X-Internal-Secret): kept for backwards compatibility. */
    @GetMapping("/users/resolve-id")
    @PreAuthorize("hasRole('SYSTEM')")
    public UUID resolveId(@RequestParam("username") String username) {
        return authService.resolveUser(username).id();
    }
}
