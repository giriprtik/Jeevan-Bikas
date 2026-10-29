package com.hospital.opd.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "opd_doctor")
public class OpdDoctor {

    @Id
    @Column(name = "id", length = 50)
    private String id; // Matches VARCHAR(50) PRIMARY KEY (e.g., 'DOC-77')

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "specialization", nullable = false, length = 100)
    private String specialization;

    @Column(name = "is_available")
    private Boolean isAvailable = true; // Maps to your BOOLEAN DEFAULT TRUE column

    // Empty default constructor required by JPA/Hibernate
    public OpdDoctor() {
    }

    // Full argument constructor for easy code setup
    public OpdDoctor(String id, String name, String specialization, Boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.isAvailable = isAvailable;
    }

    // Explicit Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
}
