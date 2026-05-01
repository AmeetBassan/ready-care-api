package com.readycare.api.controller;

import com.readycare.api.dto.*;
import com.readycare.api.entity.AvailabilitySlotStatus;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.AvailabilityService;
import com.readycare.api.service.ReviewService;
import com.readycare.api.service.SearchService;
import com.readycare.api.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/professionals")
public class ProfessionalController {

    private final AccountService accountService;
    private final AvailabilityService availabilityService;
    private final VerificationService verificationService;
    private final SearchService searchService;
    private final ReviewService reviewService;

    public ProfessionalController(
            AccountService accountService,
            AvailabilityService availabilityService,
            VerificationService verificationService,
            SearchService searchService,
            ReviewService reviewService
    ) {
        this.accountService = accountService;
        this.availabilityService = availabilityService;
        this.verificationService = verificationService;
        this.searchService = searchService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public List<ProfessionalResponse> getProfessionals() {
        return accountService.getProfessionals();
    }

    @GetMapping("/{professionalId}")
    public ProfessionalResponse getProfessional(@PathVariable UUID professionalId) {
        return accountService.getProfessional(professionalId);
    }

    @GetMapping("/me")
    public ProfessionalResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return accountService.getProfessional(currentUserId(jwt));
    }

    @GetMapping("/{professionalId}/address")
    public AddressResponse getProfessionalAddress(@PathVariable UUID professionalId) {
        return accountService.getProfessionalPrimaryAddress(professionalId);
    }

    @PutMapping("/{professionalId}")
    public ProfessionalResponse updateProfessional(
            @PathVariable UUID professionalId,
            @RequestBody UpdateProfessionalRequest request
    ) {
        return accountService.updateProfessional(professionalId, request);
    }

    @PatchMapping("/me")
    public ProfessionalResponse updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateProfessionalRequest request
    ) {
        return accountService.updateProfessional(currentUserId(jwt), request);
    }

    @DeleteMapping("/{professionalId}")
    public void deleteProfessional(@PathVariable UUID professionalId) {
        accountService.deleteProfessional(professionalId);
    }

    @PutMapping("/{professionalId}/availability")
    public List<AvailabilitySlotResponse> upsertAvailability(
            @PathVariable UUID professionalId,
            @Valid @RequestBody UpsertAvailabilityRequest request
    ) {
        return availabilityService.upsertAvailability(professionalId, request);
    }

    @GetMapping("/{professionalId}/availability")
    public List<AvailabilitySlotResponse> listAvailability(
            @PathVariable UUID professionalId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return availabilityService.listAvailability(professionalId, from, to);
    }

    @PatchMapping("/{professionalId}/availability/{slotId}")
    public void setSlotStatus(
            @PathVariable UUID professionalId,
            @PathVariable UUID slotId,
            @RequestParam AvailabilitySlotStatus status
    ) {
        availabilityService.setSlotStatus(professionalId, slotId, status);
    }

    @DeleteMapping("/{professionalId}/availability/{slotId}")
    public ApiMessageResponse deleteAvailabilitySlot(
            @PathVariable UUID professionalId,
            @PathVariable UUID slotId
    ) {
        availabilityService.deleteAvailabilitySlot(professionalId, slotId);
        return new ApiMessageResponse("Availability slot deleted");
    }

    @PostMapping("/me/submit-application")
    public ProfessionalResponse submitApplication(@AuthenticationPrincipal Jwt jwt) {
        return accountService.submitProfessionalApplication(currentUserId(jwt));
    }

    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse uploadMyDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("documentTypeId") UUID documentTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "expiryDate", required = false) String expiryDate
    ) {
        return verificationService.uploadDocument(currentUserId(jwt), documentTypeId, file, expiryDate);
    }

    @GetMapping("/me/documents")
    public List<DocumentResponse> getMyDocuments(@AuthenticationPrincipal Jwt jwt) {
        return verificationService.getDocuments(currentUserId(jwt));
    }

    @GetMapping("/me/documents/{documentId}/file")
    public ResponseEntity<byte[]> getMyDocumentFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID documentId
    ) {
        DocumentFileResponse file = verificationService.getDocumentFileForProfessional(currentUserId(jwt), documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.bytes());
    }

    @PostMapping(value = "/{professionalId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponse uploadDocument(
            @PathVariable UUID professionalId,
            @RequestPart("documentTypeId") UUID documentTypeId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "expiryDate", required = false) String expiryDate
    ) {
        return verificationService.uploadDocument(professionalId, documentTypeId, file, expiryDate);
    }

    @GetMapping("/{professionalId}/documents")
    public List<DocumentResponse> getDocuments(@PathVariable UUID professionalId) {
        return verificationService.getDocuments(professionalId);
    }

    @GetMapping("/search")
    public List<ProfessionalSearchResponse> search(
            @RequestParam String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startTs,
            @RequestParam int durationHours
    ) {
        return searchService.search(city, startTs, durationHours);
    }

    @GetMapping("/{professionalId}/reviews")
    public ProfessionalReviewsResponse getProfessionalReviews(@PathVariable UUID professionalId) {
        return reviewService.getProfessionalReviews(professionalId);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
