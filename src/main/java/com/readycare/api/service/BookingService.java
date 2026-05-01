package com.readycare.api.service;

import com.readycare.api.dto.BookingResponse;
import com.readycare.api.dto.CancelBookingRequest;
import com.readycare.api.dto.CreateBookingRequest;
import com.readycare.api.entity.*;
import com.readycare.api.exception.BadRequestException;
import com.readycare.api.exception.NotFoundException;
import com.readycare.api.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSlotRepository bookingSlotRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProfessionalProfileRepository professionalProfileRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final int fullRefundHoursThreshold;
    private final boolean allowEarlyComplete;

    public BookingService(
            BookingRepository bookingRepository,
            BookingSlotRepository bookingSlotRepository,
            AvailabilitySlotRepository availabilitySlotRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            ProfessionalProfileRepository professionalProfileRepository,
            PaymentService paymentService,
            NotificationService notificationService,
            @Value("${app.cancellation.full-refund-hours-threshold:48}") int fullRefundHoursThreshold,
            @Value("${app.booking.allow-early-complete:false}") boolean allowEarlyComplete
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingSlotRepository = bookingSlotRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.professionalProfileRepository = professionalProfileRepository;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
        this.fullRefundHoursThreshold = fullRefundHoursThreshold;
        this.allowEarlyComplete = allowEarlyComplete;
    }

    @Transactional
    public BookingResponse createBooking(CreateBookingRequest request) {
        User client = getUserByType(request.clientId(), UserType.CLIENT);
        User professional = getUserByType(request.professionalId(), UserType.PROFESSIONAL);
        ProfessionalProfile profile = professionalProfileRepository.findById(request.professionalId())
                .orElseThrow(() -> new NotFoundException("Professional profile not found"));

        if (profile.getOverallVerificationStatus() != VerificationStatus.APPROVED) {
            throw new BadRequestException("Professional is not approved");
        }

        Address address = addressRepository.findById(request.addressId())
                .orElseThrow(() -> new NotFoundException("Address not found"));
        if (!address.getUser().getId().equals(client.getId())) {
            throw new BadRequestException("Address must belong to client");
        }

        validateBookingTimes(request.startTs(), request.endTs());
        long hours = Duration.between(request.startTs(), request.endTs()).toHours();

        List<AvailabilitySlot> slots = availabilitySlotRepository.findByProfessionalIdAndStartTsBetweenAndStatus(
                professional.getId(),
                request.startTs(),
                request.endTs().minusSeconds(1),
                AvailabilitySlotStatus.FREE
        );

        if (slots.size() != hours) {
            throw new BadRequestException("Requested time is not fully available");
        }

        Booking booking = new Booking();
        booking.setClient(client);
        booking.setProfessional(professional);
        booking.setAddress(address);
        booking.setStartTs(request.startTs());
        booking.setEndTs(request.endTs());
        booking.setStatus(BookingStatus.REQUESTED);
        booking.setClientNotes(request.clientNotes());

        BigDecimal hourlyRate = profile.getHourlyRateOfficeHours() == null ? BigDecimal.ZERO : profile.getHourlyRateOfficeHours();
        BigDecimal price = hourlyRate.multiply(BigDecimal.valueOf(hours));
        booking.setPrice(price);
        booking.setCurrency(CurrencyCode.GBP);

        booking = bookingRepository.save(booking);

        for (AvailabilitySlot slot : slots) {
            slot.setStatus(AvailabilitySlotStatus.BOOKED);
            availabilitySlotRepository.save(slot);

            BookingSlot bookingSlot = new BookingSlot();
            bookingSlot.setBooking(booking);
            bookingSlot.setAvailabilitySlot(slot);
            bookingSlot.setId(new BookingSlotId(booking.getId(), slot.getId()));
            bookingSlotRepository.save(bookingSlot);
        }

        paymentService.createMockPaidPayment(booking, price);
        notificationService.sendBookingCreatedNotifications(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse confirmBooking(UUID professionalId, UUID bookingId) {
        Booking booking = getBookingByProfessional(bookingId, professionalId);
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BadRequestException("Only requested bookings can be confirmed");
        }
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse rejectBooking(UUID professionalId, UUID bookingId, String reason) {
        Booking booking = getBookingByProfessional(bookingId, professionalId);
        if (booking.getStatus() != BookingStatus.REQUESTED) {
            throw new BadRequestException("Only requested bookings can be rejected");
        }
        booking.setStatus(BookingStatus.REJECTED);
        booking.setProfessionalNotes(reason);
        booking = bookingRepository.save(booking);

        releaseBookingSlots(booking);
        paymentService.refundFullIfPaid(booking, reason);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(UUID bookingId, CancelBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        UUID actorUserId = request.actorUserId();
        if (!booking.getClient().getId().equals(actorUserId) && !booking.getProfessional().getId().equals(actorUserId)) {
            throw new BadRequestException("Actor does not belong to booking");
        }

        if (booking.getStatus() != BookingStatus.REQUESTED && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only requested or confirmed bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(OffsetDateTime.now());
        booking.setCancelledBy(request.cancelledBy());
        booking = bookingRepository.save(booking);
        releaseBookingSlots(booking);

        if (request.cancelledBy() == UserType.CLIENT) {
            long hoursUntilStart = Duration.between(OffsetDateTime.now(), booking.getStartTs()).toHours();
            if (hoursUntilStart < fullRefundHoursThreshold) {
                paymentService.doNotRefund(booking, "CLIENT_CANCELLED_WITHIN_THRESHOLD");
            } else {
                paymentService.refundFullIfPaid(booking, "CLIENT_CANCELLED_OUTSIDE_THRESHOLD");
            }
        } else {
            paymentService.refundFullIfPaid(booking, "CANCELLED_BY_" + request.cancelledBy());
        }

        notificationService.sendBookingCancelledNotifications(booking);
        return toResponse(booking);
    }

    @Transactional
    public BookingResponse markNoShow(UUID clientId, UUID bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getClient().getId().equals(clientId)) {
            throw new BadRequestException("Only client can mark no-show");
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("No-show is allowed only for confirmed bookings");
        }

        booking.setStatus(BookingStatus.NO_SHOW);
        return toResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse completeBooking(UUID professionalId, UUID bookingId) {
        Booking booking = getBookingByProfessional(bookingId, professionalId);
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Only confirmed bookings can be completed");
        }
        if (!allowEarlyComplete && OffsetDateTime.now().isBefore(booking.getEndTs())) {
            throw new BadRequestException("Booking can only be completed after end time");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        return toResponse(bookingRepository.save(booking));
    }

    public List<BookingResponse> getClientBookings(UUID clientId) {
        getUserByType(clientId, UserType.CLIENT);
        return bookingRepository.findByClientIdOrderByStartTsDesc(clientId).stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getProfessionalBookings(UUID professionalId) {
        getUserByType(professionalId, UserType.PROFESSIONAL);
        return bookingRepository.findByProfessionalIdOrderByStartTsDesc(professionalId).stream().map(this::toResponse).toList();
    }

    public List<BookingResponse> getMyBookings(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getType() == UserType.CLIENT) {
            return getClientBookings(userId);
        }
        if (user.getType() == UserType.PROFESSIONAL) {
            return getProfessionalBookings(userId);
        }
        return getAllBookings();
    }

    public BookingResponse getBooking(UUID bookingId, UUID userId, UserType userType) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (userType != UserType.ADMIN
                && !booking.getClient().getId().equals(userId)
                && !booking.getProfessional().getId().equals(userId)) {
            throw new BadRequestException("Booking does not belong to current user");
        }
        return toResponse(booking);
    }

    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAllByOrderByStartTsDesc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public int autoRejectExpiredPendingBookings(OffsetDateTime now) {
        List<Booking> toReject = bookingRepository.findByStatusAndStartTsBefore(BookingStatus.REQUESTED, now);
        for (Booking booking : toReject) {
            booking.setStatus(BookingStatus.REJECTED);
            booking.setProfessionalNotes("NOT_ACCEPTED_IN_TIME");
            bookingRepository.save(booking);
            releaseBookingSlots(booking);
            paymentService.refundFullIfPaid(booking, "NOT_ACCEPTED_IN_TIME");
        }
        return toReject.size();
    }

    @Transactional
    public int sendOneHourReminders(OffsetDateTime now) {
        OffsetDateTime from = now.plusMinutes(55);
        OffsetDateTime to = now.plusMinutes(65);
        List<Booking> due = bookingRepository.findByStatusAndStartTsBetweenAndReminderSentAtIsNull(
                BookingStatus.CONFIRMED,
                from,
                to
        );
        for (Booking booking : due) {
            notificationService.sendBookingReminderNotifications(booking);
            booking.setReminderSentAt(now);
            bookingRepository.save(booking);
        }
        return due.size();
    }

    private Booking getBookingByProfessional(UUID bookingId, UUID professionalId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!booking.getProfessional().getId().equals(professionalId)) {
            throw new BadRequestException("Booking does not belong to professional");
        }
        return booking;
    }

    private User getUserByType(UUID userId, UserType type) {
        User user = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getType() != type) {
            throw new BadRequestException("User is not of type " + type);
        }
        return user;
    }

    private void validateBookingTimes(OffsetDateTime startTs, OffsetDateTime endTs) {
        if (!startTs.isBefore(endTs)) {
            throw new BadRequestException("End must be after start");
        }
        if (startTs.getMinute() != 0 || startTs.getSecond() != 0 || endTs.getMinute() != 0 || endTs.getSecond() != 0) {
            throw new BadRequestException("Booking times must be aligned on full hours");
        }
        long seconds = Duration.between(startTs, endTs).getSeconds();
        if (seconds % 3600 != 0) {
            throw new BadRequestException("Booking duration must be whole hours");
        }
    }

    private void releaseBookingSlots(Booking booking) {
        List<BookingSlot> bookingSlots = bookingSlotRepository.findByBooking_Id(booking.getId());
        for (BookingSlot bookingSlot : bookingSlots) {
            AvailabilitySlot slot = bookingSlot.getAvailabilitySlot();
            slot.setStatus(AvailabilitySlotStatus.FREE);
            availabilitySlotRepository.save(slot);
        }
    }

    public BookingResponse toResponse(Booking booking) {
        Payment payment = paymentService.findByBookingId(booking);
        return new BookingResponse(
                booking.getId(),
                booking.getClient().getId(),
                booking.getProfessional().getId(),
                booking.getAddress().getId(),
                booking.getStartTs(),
                booking.getEndTs(),
                booking.getStatus(),
                booking.getPrice(),
                booking.getCurrency(),
                booking.getClientNotes(),
                booking.getProfessionalNotes(),
                payment != null ? payment.getStatus() : null,
                payment != null ? payment.getRefundedAmount() : null,
                booking.getCancelledBy()
        );
    }
}
