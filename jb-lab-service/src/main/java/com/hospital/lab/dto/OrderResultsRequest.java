package com.hospital.lab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderResultsRequest(@NotEmpty @Valid List<Entry> results) {

    public record Entry(
            @NotBlank String testCode,
            @NotBlank @Size(max = 100) String parameterName,
            @NotBlank @Size(max = 50) String observedValue,
            Boolean abnormal,          // manual flag, used for qualitative values (e.g. urine protein "++")
            String technicianNotes) {}
}