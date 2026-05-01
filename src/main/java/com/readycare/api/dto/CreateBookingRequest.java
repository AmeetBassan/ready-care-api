package com.readycare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID clientId,
        @NotNull UUID professionalId,
        @NotNull UUID addressId,
        @NotNull OffsetDateTime startTs,
        @NotNull OffsetDateTime endTs,
        String clientNotes
) {
}
