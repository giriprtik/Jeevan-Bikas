package com.hospital.opd.dto;

import com.hospital.opd.model.Gender;
import com.hospital.opd.model.VisitPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** New patient + their first visit, captured in one step at the registration desk. */
public class PatientRegisterRequest {

    @NotBlank(message = "is required")
    @Size(max = 150)
    private String fullName;

    @NotNull(message = "is required")
    @PastOrPresent(message = "cannot be in the future")
    private LocalDate dateOfBirth;

    private boolean dobEstimated = false;

    @NotNull(message = "is required (MALE, FEMALE or OTHER)")
    private Gender gender;

    @NotBlank(message = "is required")
    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 5)
    private String bloodGroup;

    private String allergies;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    // ---- first visit ----

    @NotBlank(message = "is required")
    @Size(max = 500)
    private String chiefComplaint;

    @NotNull(message = "is required (NORMAL, URGENT or EMERGENCY)")
    private VisitPriority priority = VisitPriority.NORMAL;

    @NotBlank(message = "is required")
    @Size(max = 50)
    private String assignedDoctorUsername;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public boolean isDobEstimated() { return dobEstimated; }
    public void setDobEstimated(boolean dobEstimated) { this.dobEstimated = dobEstimated; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }
    public String getChiefComplaint() { return chiefComplaint; }
    public void setChiefComplaint(String chiefComplaint) { this.chiefComplaint = chiefComplaint; }
    public VisitPriority getPriority() { return priority; }
    public void setPriority(VisitPriority priority) { this.priority = priority; }
    public String getAssignedDoctorUsername() { return assignedDoctorUsername; }
    public void setAssignedDoctorUsername(String assignedDoctorUsername) { this.assignedDoctorUsername = assignedDoctorUsername; }
}
