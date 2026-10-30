package com.hospital.auth.controller;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.dto.UserRegisterRequest;
import com.hospital.auth.model.UserAccount;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import com.hospital.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController { // Streamlined Boundary Layer

    private final AuthService authService; // Direct connection hook to Control layer

    // Clean constructor injection bypassing Lombok overhead
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            // Hand off to Control engine to check credentials and generate token
            LoginResponse responsePayload = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(responsePayload);
        } catch (RuntimeException ex) {
            // Catches authentication failures (deactivated accounts, bad credentials)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "error", "Unauthorized", 
                            "message", ex.getMessage()
                    ));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegisterRequest registerRequest) {
        try {
            // Hand off to Control engine to run validations and persist entity
            UserAccount createdAccount = authService.registerNewUser(registerRequest);
            
            // Build the clean web response payload out of the returned Entity state
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully!",
                    "username", createdAccount.getUsername(),
                    "role", createdAccount.getRole()
            ));
        } catch (IllegalArgumentException ex) {
            // Translates specific Control layer validation errors into standard 400 Bad Requests
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "error", "Bad Request", 
                            "message", ex.getMessage()
                    ));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        
        // Return a secure response containing the user details pulled from the token context
        return ResponseEntity.ok(Map.of(
            "status", "Success",
            "message", "Profile data retrieved securely.",
            "data", Map.of(
                "username", userDetails.getUsername(),
                "authorities", userDetails.getAuthorities(),
                "accountNonExpired", userDetails.isAccountNonExpired()
            )
        ));
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPing() {
        return "{\"message\": \"admin only\"}"; // Hardcoded clean JSON payload bypasses Jackson mapping checks
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String m) { super(m); }
    }
}

