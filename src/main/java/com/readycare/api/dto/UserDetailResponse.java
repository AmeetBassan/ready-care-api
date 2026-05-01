package com.readycare.api.dto;

import com.readycare.api.entity.UserType;

import java.util.UUID;

public record UserDetailResponse(
        UUID id,
        UserType type,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        boolean active,
        UUID primaryAddressId,
        String city,
        ProfessionalProfileResponse professionalProfile,
        ClientProfileResponse clientProfile,
        AdminProfileResponse adminProfile
) {
}
