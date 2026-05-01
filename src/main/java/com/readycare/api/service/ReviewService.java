package com.readycare.api.service;

import com.readycare.api.dto.ProfessionalReviewsResponse;
import com.readycare.api.dto.ReviewRequest;
import com.readycare.api.dto.ReviewResponse;
import com.readycare.api.entity.Booking;
import com.readycare.api.entity.BookingStatus;
import com.readycare.api.entity.Review;
import com.readycare.api.entity.User;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.exception.NotFoundException;
import com.readycare.api.repository.BookingRepository;
import com.readycare.api.repository.ReviewRepository;
import com.readycare.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ReviewService {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewService(
            BookingRepository bookingRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse createReview(UUID bookingId, UUID reviewerId, ReviewRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("Reviews can only be created for completed bookings");
        }

        User reviewer = userRepository.findById(reviewerId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found"));
        User reviewee = getReviewee(booking, reviewerId);

        reviewRepository.findByBooking_IdAndReviewer_Id(bookingId, reviewerId)
                .ifPresent(existing -> {
                    throw new BadRequestException("Reviewer has already reviewed this booking");
                });

        Review review = new Review();
        review.setBooking(booking);
        review.setReviewer(reviewer);
        review.setReviewee(reviewee);
        review.setRating(request.rating());
        review.setComment(request.comment());

        return toResponse(reviewRepository.save(review));
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getBookingReviews(UUID bookingId) {
        return reviewRepository.findByBooking_Id(bookingId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(UUID userId) {
        return reviewRepository.findByReviewee_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProfessionalReviewsResponse getProfessionalReviews(UUID professionalId) {
        List<ReviewResponse> reviews = reviewRepository.findByReviewee_IdOrderByCreatedAtDesc(professionalId)
                .stream()
                .map(this::toResponse)
                .toList();

        double average = reviews.stream()
                .mapToInt(ReviewResponse::rating)
                .average()
                .orElse(0);

        return new ProfessionalReviewsResponse(average, reviews.size(), reviews);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private User getReviewee(Booking booking, UUID reviewerId) {
        if (booking.getClient().getId().equals(reviewerId)) {
            return booking.getProfessional();
        }
        if (booking.getProfessional().getId().equals(reviewerId)) {
            return booking.getClient();
        }
        throw new BadRequestException("Reviewer must be part of the booking");
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getBooking().getId(),
                review.getReviewer().getId(),
                review.getReviewee().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
