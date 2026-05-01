package com.readycare.api.controller;

import com.readycare.api.dto.*;
import com.readycare.api.entity.UserType;
import com.readycare.api.service.BookingService;
import com.readycare.api.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final ReviewService reviewService;

    public BookingController(BookingService bookingService, ReviewService reviewService) {
        this.bookingService = bookingService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public BookingResponse createBooking(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/client/{clientId}")
    public List<BookingResponse> getClientBookings(@PathVariable UUID clientId) {
        return bookingService.getClientBookings(clientId);
    }

    @GetMapping("/professional/{professionalId}")
    public List<BookingResponse> getProfessionalBookings(@PathVariable UUID professionalId) {
        return bookingService.getProfessionalBookings(professionalId);
    }

    @GetMapping("/me")
    public List<BookingResponse> getMyBookings(@AuthenticationPrincipal Jwt jwt) {
        return bookingService.getMyBookings(currentUserId(jwt));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse getBooking(@PathVariable UUID bookingId, @AuthenticationPrincipal Jwt jwt) {
        return bookingService.getBooking(bookingId, currentUserId(jwt), currentUserType(jwt));
    }

    @PostMapping("/{bookingId}/confirm")
    public BookingResponse confirmBooking(@PathVariable UUID bookingId, @RequestParam UUID professionalId) {
        return bookingService.confirmBooking(professionalId, bookingId);
    }

    @PostMapping("/{bookingId}/accept")
    public BookingResponse acceptBooking(@PathVariable UUID bookingId, @RequestParam UUID professionalId) {
        return bookingService.confirmBooking(professionalId, bookingId);
    }

    @PostMapping("/{bookingId}/reject")
    public BookingResponse rejectBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID professionalId,
            @Valid @RequestBody RejectBookingRequest request
    ) {
        return bookingService.rejectBooking(professionalId, bookingId, request.reason());
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable UUID bookingId, @Valid @RequestBody CancelBookingRequest request) {
        return bookingService.cancelBooking(bookingId, request);
    }

    @PostMapping("/{bookingId}/no-show")
    public BookingResponse markNoShow(@PathVariable UUID bookingId, @RequestParam UUID clientId) {
        return bookingService.markNoShow(clientId, bookingId);
    }

    @PostMapping("/{bookingId}/complete")
    public BookingResponse completeBooking(@PathVariable UUID bookingId, @RequestParam UUID professionalId) {
        return bookingService.completeBooking(professionalId, bookingId);
    }

    @PostMapping("/{bookingId}/reviews")
    public ReviewResponse createReview(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewRequest request
    ) {
        return reviewService.createReview(bookingId, currentUserId(jwt), request);
    }

    @GetMapping("/{bookingId}/reviews")
    public List<ReviewResponse> getBookingReviews(@PathVariable UUID bookingId) {
        return reviewService.getBookingReviews(bookingId);
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private UserType currentUserType(Jwt jwt) {
        return UserType.valueOf(jwt.getClaimAsString("userType"));
    }
}
