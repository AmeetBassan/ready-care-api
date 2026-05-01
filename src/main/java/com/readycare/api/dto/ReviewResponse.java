package com.readycare.api.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReviewResponse(
        UUID id,
        UUID bookingId,
        UUID reviewerUserId,
        UUID revieweeUserId,
        Integer rating,
        String comment,
        OffsetDateTime createdAt
) {
}
