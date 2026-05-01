package com.readycare.api.dto;

public record DocumentFileResponse(
        byte[] bytes,
        String contentType,
        String fileName
) {
}
