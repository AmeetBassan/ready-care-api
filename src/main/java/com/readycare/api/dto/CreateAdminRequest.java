package com.readycare.api.dto;

import com.readycare.api.entity.GenderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAdminRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull GenderType gender,
        @NotNull LocalDate dob,
        @NotBlank @Email String email,
        String phoneNumber,
        @NotBlank String password,
        @NotNull @Valid AddressRequest primaryAddress
) {
}
