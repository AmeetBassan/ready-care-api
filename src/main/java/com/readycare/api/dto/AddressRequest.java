package com.readycare.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        String label,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String postcode,
        String country
) {
}
