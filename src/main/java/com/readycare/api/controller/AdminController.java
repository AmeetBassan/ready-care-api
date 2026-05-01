package com.readycare.api.controller;

import com.readycare.api.dto.*;
import com.readycare.api.entity.UserType;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.BookingService;
import com.readycare.api.service.ReviewService;
import com.readycare.api.service.VerificationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final VerificationService verificationService;
    private final AccountService accountService;
    private final BookingService bookingService;
    private final ReviewService reviewService;

    public AdminController(
            VerificationService verificationService,
            AccountService accountService,
            BookingService bookingService,
            ReviewService reviewService
    ) {
        this.verificationService = verificationService;
        this.accountService = accountService;
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    @PatchMapping("/documents/{documentId}/review")
    public DocumentResponse reviewDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody ReviewDocumentRequest request
    ) {

        return verificationService.reviewDocument(documentId, request);
    }

    @GetMapping("/professionals/pending")
    public List<ProfessionalResponse> getPendingProfessionals() {
        return accountService.getPendingProfessionals();
    }

    @GetMapping("/users")
    public List<UserDetailResponse> getUsers(@RequestParam(required = false) UserType userType) {
        return accountService.getUsers(userType);
    }

    @GetMapping("/users/{userId}")
    public UserDetailResponse getUser(@PathVariable UUID userId) {
        return accountService.getUser(userId);
    }

    @PatchMapping("/users/{userId}/status")
    public UserDetailResponse updateUserStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return accountService.updateUserStatus(userId, request);
    }

    @GetMapping("/professionals/{professionalId}")
    public ProfessionalResponse getProfessionalApplication(@PathVariable UUID professionalId) {
        return accountService.getProfessional(professionalId);
    }

    @PostMapping("/professionals/{professionalId}/approve")
    public ProfessionalResponse approveProfessional(
            @PathVariable UUID professionalId,
            @RequestBody(required = false) AdminProfessionalReviewRequest request
    ) {
        return accountService.approveProfessional(professionalId);
    }

    @PostMapping("/professionals/{professionalId}/reject")
    public ProfessionalResponse rejectProfessional(
            @PathVariable UUID professionalId,
            @RequestBody(required = false) AdminProfessionalReviewRequest request
    ) {
        return accountService.rejectProfessional(professionalId);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/bookings/{bookingId}")
    public BookingResponse getBooking(@PathVariable UUID bookingId) {
        return bookingService.getBooking(bookingId, UUID.randomUUID(), UserType.ADMIN);
    }

    @GetMapping("/reviews")
    public List<ReviewResponse> getReviews() {
        return reviewService.getAllReviews();
    }

    @GetMapping("/documents/{documentId}/file")
    public ResponseEntity<byte[]> getDocumentFile(@PathVariable UUID documentId) {
        DocumentFileResponse file = verificationService.getDocumentFileForAdmin(documentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                .body(file.bytes());
    }

}
