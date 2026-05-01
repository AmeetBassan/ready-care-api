package com.readycare.api.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class BookingTimeoutJob {

    private final BookingService bookingService;

    public BookingTimeoutJob(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Scheduled(fixedDelayString = "${app.booking-timeout.fixed-delay-ms:60000}")
    public void autoRejectExpiredPendingBookings() {
        bookingService.autoRejectExpiredPendingBookings(OffsetDateTime.now());
    }

    @Scheduled(fixedDelayString = "${app.booking-reminder.fixed-delay-ms:60000}")
    public void sendOneHourReminders() {
        bookingService.sendOneHourReminders(OffsetDateTime.now());
    }
}
