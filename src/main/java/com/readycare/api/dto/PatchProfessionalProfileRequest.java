package com.readycare.api.dto;

import java.math.BigDecimal;

public record PatchProfessionalProfileRequest(
        String bio,
        Integer yearsExperience,
        BigDecimal hourlyRateOfficeHours,
        BigDecimal hourlyRateOutOfOfficeHours
) {
}
