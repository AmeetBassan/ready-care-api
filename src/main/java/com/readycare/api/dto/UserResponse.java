package com.readycare.api.dto;

import com.readycare.api.entity.UserType;

import java.util.UUID;

public record UserResponse(
        UUID id,
        UserType type,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        UUID primaryAddressId,
        String city
) {
}
