package com.readycare.api.dto;

import com.readycare.api.entity.AvailabilitySlotStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record UpsertAvailabilityRequest(
        @NotNull LocalDate day,
        @Min(0) @Max(23) int startHour,
        @Min(1) @Max(24) int endHour,
        @NotNull AvailabilitySlotStatus status
) {
}
