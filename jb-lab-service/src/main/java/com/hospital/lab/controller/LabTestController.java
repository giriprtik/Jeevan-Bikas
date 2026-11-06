package com.hospital.lab.controller;

import com.hospital.lab.dto.LabTestResponse;
import com.hospital.lab.dto.TestParameterResponse;
import com.hospital.lab.repository.LabTestParameterRepository;
import com.hospital.lab.repository.LabTestRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/lab/tests")
public class LabTestController {

    private final LabTestParameterRepository parameterRepository;
    private final LabTestRepository testRepository;

    public LabTestController(LabTestParameterRepository parameterRepository, LabTestRepository testRepository) {
        this.parameterRepository = parameterRepository;
        this.testRepository = testRepository;
    }

    /** Orderable test catalogue for the doctor's screen. */
    @GetMapping
    @PreAuthorize("hasAnyRole('LABTECH', 'DOCTOR', 'ADMIN')")
    public List<LabTestResponse> getCatalogue() {
        return testRepository.findByActiveTrueOrderByTestNameAsc().stream()
                .map(t -> new LabTestResponse(t.getTestCode(), t.getTestName(), t.getDepartment(), t.getCost()))
                .toList();
    }

    /** Parameter catalogue for the given tests: GET /api/v1/lab/tests/parameters?codes=CBC,LFT */
    @GetMapping("/parameters")
    @PreAuthorize("hasAnyRole('LABTECH', 'DOCTOR', 'ADMIN')")
    public List<TestParameterResponse> getParameters(@RequestParam List<String> codes) {
        List<String> normalized = codes.stream().map(c -> c.trim().toUpperCase(Locale.ROOT)).toList();
        return parameterRepository.findByTestCodes(normalized).stream()
                .map(p -> new TestParameterResponse(
                        p.getTest().getTestCode(),
                        p.getTest().getTestName(),
                        p.getParameterName(),
                        p.getUnit(),
                        p.getRefLow(),
                        p.getRefHigh()))
                .toList();
    }
}