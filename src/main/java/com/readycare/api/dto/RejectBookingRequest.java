package com.readycare.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectBookingRequest(@NotBlank String reason) {
}
