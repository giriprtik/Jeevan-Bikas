package com.hospital.opd.repository;

import com.hospital.opd.model.OpdDoctor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OpdDoctorRepository extends JpaRepository<OpdDoctor, String> {
    
    // Custom query method using your indexed field to easily find available doctors by specialization
    List<OpdDoctor> findBySpecializationAndIsAvailableTrue(String specialization);
}
