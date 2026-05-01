package com.readycare.api.repository;

import com.readycare.api.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
    Optional<Review> findByBooking_IdAndReviewer_Id(UUID bookingId, UUID reviewerId);

    List<Review> findByBooking_Id(UUID bookingId);

    List<Review> findByReviewee_Id(UUID revieweeId);

    List<Review> findByReviewee_IdOrderByCreatedAtDesc(UUID revieweeId);
}
