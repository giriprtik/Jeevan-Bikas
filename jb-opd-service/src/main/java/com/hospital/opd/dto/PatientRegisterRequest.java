package com.hospital.opd.dto;

import com.hospital.opd.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class PatientRegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private boolean dobEstimated = false;

    @NotNull(message = "Gender is required (MALE, FEMALE, or OTHER)")
    private Gender gender;

    @NotBlank(message = "Primary contact phone number is required")
    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    @Size(max = 255, message = "Address payload cannot exceed 255 characters")
    private String address;

    @Size(max = 5, message = "Blood group identifier must be short (e.g., A+, O-, B+)")
    private String bloodGroup;

    private String allergies;

    @Size(max = 150, message = "Emergency contact name cannot exceed 150 characters")
    private String emergencyContactName;

    @Size(max = 20, message = "Emergency contact phone cannot exceed 20 characters")
    private String emergencyContactPhone;

    // Getters
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public boolean isDobEstimated() { return dobEstimated; }
    public Gender getGender() { return gender; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getBloodGroup() { return bloodGroup; }
    public String getAllergies() { return allergies; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }

    // Setters
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setDobEstimated(boolean dobEstimated) { this.dobEstimated = dobEstimated; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
}
