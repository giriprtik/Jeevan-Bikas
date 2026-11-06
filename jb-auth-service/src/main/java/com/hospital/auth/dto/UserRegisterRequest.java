package com.hospital.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisterRequest {

    @NotBlank @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "may only contain letters, digits, '.', '_' and '-'")
    private String username;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @NotBlank @Size(min = 8, max = 100, message = "must be at least 8 characters")
    private String password;

    @NotBlank
    @Pattern(regexp = "^(DOCTOR|NURSE|LABTECH|ADMIN)$", message = "must be one of DOCTOR, NURSE, LABTECH, ADMIN")
    private String role;

    @NotBlank @Size(max = 100)
    private String fullName;

    @Size(max = 50)
    private String department;

    @Size(max = 50)
    private String licenseNumber;

    @Size(max = 100)
    private String specialization;
}
