package com.readycare.api.dto;

import com.readycare.api.entity.UserType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CancelBookingRequest(
        @NotNull UUID actorUserId,
        @NotNull UserType cancelledBy
) {
}
