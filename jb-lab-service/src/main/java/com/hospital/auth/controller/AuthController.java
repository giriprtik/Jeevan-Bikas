package com.hospital.auth.controller;

import com.hospital.auth.dto.LoginRequest;
import com.hospital.auth.dto.LoginResponse;
import com.hospital.auth.model.UserAccount;
import com.hospital.auth.util.JwtTokenProvider;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("auth/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        System.out.println(">>> [CONTROLLER START] Login endpoint triggered for: " + loginRequest.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
            
            System.out.println(">>> [CONTROLLER STEP] Credentials matched successfully!");
            UserAccount userDetails = (UserAccount) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(userDetails.getUsername(), userDetails.getRole());

            return ResponseEntity.ok(new LoginResponse(
                    token, 
                    "Bearer", 
                    jwtTokenProvider.getExpirationMs(), 
                    userDetails.getUsername(), 
                    userDetails.getRole()
            ));
            
        } catch (org.springframework.security.core.AuthenticationException e) {
            System.out.println(">>> [AUTH FAILURE] Bad credentials error: " + e.getMessage());
            return ResponseEntity.status(401).body(java.util.Map.of("message", "Invalid credentials"));
        } catch (Exception e) {
            System.out.println(">>> [CONTROLLER CRASH] Root Exception: " + e.getMessage());
            return ResponseEntity.status(500).body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/admin/ping")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminPing() {
        System.out.println(">>> [CONTROLLER ACCESS GRANTED] Executing adminPing method successfully!");
        return "{\"message\": \"admin only\"}"; // Hardcoded clean JSON payload bypasses Jackson mapping checks
    }

    public static class BadCredentialsException extends RuntimeException {
        public BadCredentialsException(String m) { super(m); }
    }
}

