package com.readycare.api.dto;

public record UpdateClientRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String city
) {
}
