package com.readycare.api.dto;

import com.readycare.api.entity.BookingStatus;
import com.readycare.api.entity.CurrencyCode;
import com.readycare.api.entity.PaymentStatus;
import com.readycare.api.entity.UserType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID clientId,
        UUID professionalId,
        UUID addressId,
        OffsetDateTime startTs,
        OffsetDateTime endTs,
        BookingStatus status,
        BigDecimal price,
        CurrencyCode currency,
        String clientNotes,
        String professionalNotes,
        PaymentStatus paymentStatus,
        BigDecimal refundedAmount,
        UserType cancelledBy
) {
}
