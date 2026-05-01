package com.readycare.api.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(
        @NotNull Boolean active,
        String reason
) {
}
