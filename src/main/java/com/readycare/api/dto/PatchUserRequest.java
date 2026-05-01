package com.readycare.api.dto;

public record PatchUserRequest(
        String firstName,
        String lastName,
        String phoneNumber,
        String city,
        PatchProfessionalProfileRequest professionalProfile
) {
}
