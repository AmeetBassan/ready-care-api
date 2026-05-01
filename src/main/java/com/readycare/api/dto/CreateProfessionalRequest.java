package com.readycare.api.dto;

import com.readycare.api.entity.GenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProfessionalRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull GenderType gender,
        @NotNull LocalDate dob,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotBlank String password,
        String bio,
        Integer yearsExperience,
        BigDecimal hourlyRateOfficeHours,
        BigDecimal hourlyRateOutOfOfficeHours,
        @NotNull @Valid AddressRequest primaryAddress
) {
}
