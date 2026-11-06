package com.hospital.lab.dto;

import com.hospital.lab.model.LabOrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest(@NotNull LabOrderStatus status) {}