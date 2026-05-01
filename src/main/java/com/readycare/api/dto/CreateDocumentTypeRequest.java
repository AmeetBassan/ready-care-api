package com.readycare.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentTypeRequest(
        @NotBlank String name,
        String description,
        boolean required,
        boolean hasExpiry
) {
}
