package com.readycare.api.dto;

import java.math.BigDecimal;

public record UpdateProfessionalRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String bio,
        Integer yearsExperience,
        BigDecimal hourlyRateOfficeHours,
        BigDecimal hourlyRateOutOfOfficeHours,
        String city
) {
}
