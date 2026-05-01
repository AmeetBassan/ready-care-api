package com.readycare.api.dto;

public record PatchDocumentTypeRequest(
        String name,
        String description,
        Boolean required,
        Boolean hasExpiry
) {
}
