package com.readycare.api.dto;

import com.readycare.api.entity.VerificationStatus;

import java.time.LocalDate;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID professionalId,
        UUID documentTypeId,
        String documentTypeName,
        String fileStorageKey,
        VerificationStatus status,
        LocalDate expiryDate,
        String rejectionReason
) {
}
