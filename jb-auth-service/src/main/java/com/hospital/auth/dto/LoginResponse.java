package com.hospital.auth.dto;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresInMs;
    private String username;
    private String role;

    public LoginResponse() {}

    public LoginResponse(String accessToken, String tokenType, long expiresInMs, String username, String role) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresInMs = expiresInMs;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInMs() { return expiresInMs; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
