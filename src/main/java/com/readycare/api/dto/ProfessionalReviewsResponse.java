package com.readycare.api.dto;

import java.util.List;

public record ProfessionalReviewsResponse(
        double averageRating,
        long reviewCount,
        List<ReviewResponse> reviews
) {
}
