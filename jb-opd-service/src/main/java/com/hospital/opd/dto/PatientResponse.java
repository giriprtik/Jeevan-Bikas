package com.hospital.opd.dto;

import com.hospital.opd.model.Gender;
import java.time.LocalDate;

public class PatientResponse {
    private Long id;
    private String mrn;
    private String fullName;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String phone;

    public PatientResponse(Long id, String mrn, String fullName, LocalDate dateOfBirth, Gender gender, String phone) {
        this.id = id;
        this.mrn = mrn;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phone = phone;
    }

    // Getters
    public Long getId() { return id; }
    public String getMrn() { return mrn; }
    public String getFullName() { return fullName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public String getPhone() { return phone; }
}
