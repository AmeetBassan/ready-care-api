package com.readycare.api.dto;

import com.readycare.api.entity.AvailabilitySlotStatus;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record AvailabilityRequest(
        @NotNull OffsetDateTime startTs,
        @NotNull OffsetDateTime endTs,
        @NotNull AvailabilitySlotStatus status
) {
}
