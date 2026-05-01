package com.readycare.api.dto;

import com.readycare.api.entity.AvailabilitySlotStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AvailabilitySlotResponse(
        UUID id,
        OffsetDateTime startTs,
        AvailabilitySlotStatus status
) {
}
