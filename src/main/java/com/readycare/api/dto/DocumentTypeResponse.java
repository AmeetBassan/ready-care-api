package com.readycare.api.dto;

import java.util.UUID;

public record DocumentTypeResponse(
        UUID id,
        String name,
        String description,
        boolean required,
        boolean hasExpiry
) {
}
