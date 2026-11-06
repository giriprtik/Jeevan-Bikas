package com.hospital.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String username,
        String fullName,
        String role) {}
