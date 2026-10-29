package com.hospital.auth.dto;

public class LoginRequest {
    private String username;
    private String password;

    // CRITICAL FIX: Jackson demands an explicit default constructor to initialize the object
    public LoginRequest() {
    }

    // Full Arguments Constructor
    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Explicit Getters and Setters for Jackson to bind JSON fields
    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
