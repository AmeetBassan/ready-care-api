package com.readycare.api.service;

import com.readycare.api.entity.Booking;
import com.readycare.api.entity.UserType;
import com.readycare.api.service.email.EmailService;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final EmailService emailService;

    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void sendRegistrationConfirmation(String firstName, String email, UserType userType) {
        String subject = "ReadyCare account created";
        String body = """
                Hi %s,

                Your ReadyCare %s account has been created successfully.
                """.formatted(firstName, userType.name().toLowerCase());
        emailService.sendEmail(email, subject, body);
    }

    public void sendBookingCreatedNotifications(Booking booking) {
        String clientSubject = "Booking confirmation";
        String clientBody = """
                Hi %s,

                Your booking request has been created.
                Booking ID: %s
                Start: %s
                End: %s
                """.formatted(
                booking.getClient().getFirstName(),
                booking.getId(),
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getClient().getEmail(), clientSubject, clientBody);

        String professionalSubject = "New booking request";
        String professionalBody = """
                Hi %s,

                You have received a new booking request.
                Booking ID: %s
                Start: %s
                End: %s
                """.formatted(
                booking.getProfessional().getFirstName(),
                booking.getId(),
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getProfessional().getEmail(), professionalSubject, professionalBody);
    }

    public void sendBookingCancelledNotifications(Booking booking) {
        String cancelledBy = booking.getCancelledBy() == null ? "UNKNOWN" : booking.getCancelledBy().name();
        String subject = "Booking cancelled";

        String clientBody = """
                Hi %s,

                Booking %s has been cancelled by %s.
                Start: %s
                End: %s
                """.formatted(
                booking.getClient().getFirstName(),
                booking.getId(),
                cancelledBy,
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getClient().getEmail(), subject, clientBody);

        String professionalBody = """
                Hi %s,

                Booking %s has been cancelled by %s.
                Start: %s
                End: %s
                """.formatted(
                booking.getProfessional().getFirstName(),
                booking.getId(),
                cancelledBy,
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getProfessional().getEmail(), subject, professionalBody);
    }

    public void sendBookingReminderNotifications(Booking booking) {
        String subject = "Booking reminder (1 hour)";

        String clientBody = """
                Hi %s,

                Reminder: your booking starts in about 1 hour.
                Booking ID: %s
                Start: %s
                End: %s
                """.formatted(
                booking.getClient().getFirstName(),
                booking.getId(),
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getClient().getEmail(), subject, clientBody);

        String professionalBody = """
                Hi %s,

                Reminder: you have a booking starting in about 1 hour.
                Booking ID: %s
                Start: %s
                End: %s
                """.formatted(
                booking.getProfessional().getFirstName(),
                booking.getId(),
                booking.getStartTs(),
                booking.getEndTs()
        );
        emailService.sendEmail(booking.getProfessional().getEmail(), subject, professionalBody);
    }
}
