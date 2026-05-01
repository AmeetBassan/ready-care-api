package com.readycare.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProfessionalSearchResponse(
        UUID professionalId,
        String firstName,
        String lastName,
        String city,
        BigDecimal hourlyRate,
        int availableHours
) {
}
