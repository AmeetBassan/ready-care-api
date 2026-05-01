package com.readycare.api.service.storage;

public record StoredObject(
        byte[] bytes,
        String contentType
) {
}
