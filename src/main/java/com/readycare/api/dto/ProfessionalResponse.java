package com.readycare.api.dto;

import com.readycare.api.entity.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ProfessionalResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UUID primaryAddressId,
        String city,
        String bio,
        Integer yearsExperience,
        BigDecimal hourlyRateOfficeHours,
        BigDecimal hourlyRateOutOfOfficeHours,
        VerificationStatus overallVerificationStatus
) {
}
