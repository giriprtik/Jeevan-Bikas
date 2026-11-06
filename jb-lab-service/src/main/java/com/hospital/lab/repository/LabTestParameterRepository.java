package com.hospital.lab.repository;

import com.hospital.lab.model.LabTestParameter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LabTestParameterRepository extends JpaRepository<LabTestParameter, Long> {

    @Query("select p from LabTestParameter p join fetch p.test t " +
           "where t.testCode in :codes order by t.testCode, p.displayOrder")
    List<LabTestParameter> findByTestCodes(@Param("codes") Collection<String> codes);
}