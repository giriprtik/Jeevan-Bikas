package com.hospital.opd.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SendToLabRequest(
        @NotEmpty(message = "select at least one test")
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        List<String> testCodes,
        @Size(max = 1000) String clinicalNotes) {}
