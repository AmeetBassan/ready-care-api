package com.readycare.api.dto;

import com.readycare.api.entity.VerificationStatus;

import java.math.BigDecimal;

public record ProfessionalProfileResponse(
        String bio,
        Integer yearsExperience,
        BigDecimal hourlyRateOfficeHours,
        BigDecimal hourlyRateOutOfOfficeHours,
        VerificationStatus overallVerificationStatus
) {
}
