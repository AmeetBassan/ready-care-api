package com.readycare.api.dto;

public record UserProfilePictureFileResponse(
        byte[] bytes,
        String contentType,
        String fileName
) {
}
