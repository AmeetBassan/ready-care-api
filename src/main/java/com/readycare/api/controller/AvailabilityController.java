package com.readycare.api.controller;

import com.readycare.api.dto.ApiMessageResponse;
import com.readycare.api.dto.AvailabilityRequest;
import com.readycare.api.dto.AvailabilitySlotResponse;
import com.readycare.api.service.AvailabilityService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public List<AvailabilitySlotResponse> createAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return availabilityService.createAvailability(currentUserId(jwt), request);
    }

    @GetMapping("/me")
    public List<AvailabilitySlotResponse> getMyAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return availabilityService.listAvailability(currentUserId(jwt), from, to);
    }

    @PatchMapping("/{slotId}")
    public AvailabilitySlotResponse updateAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID slotId,
            @Valid @RequestBody AvailabilityRequest request
    ) {
        return availabilityService.updateAvailabilitySlot(currentUserId(jwt), slotId, request);
    }

    @DeleteMapping("/{slotId}")
    public ApiMessageResponse deleteAvailability(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID slotId
    ) {
        availabilityService.deleteAvailabilitySlot(currentUserId(jwt), slotId);
        return new ApiMessageResponse("Availability slot deleted");
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
