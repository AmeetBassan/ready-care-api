package com.readycare.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReviewDocumentRequest(
        @NotNull UUID adminId,
        @NotNull Boolean approve,
        String reason
) {
}
