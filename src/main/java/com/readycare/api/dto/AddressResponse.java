package com.readycare.api.dto;

import java.util.UUID;

public record AddressResponse(
        UUID id,
        String label,
        String line1,
        String line2,
        String city,
        String postcode,
        String country
) {
}
