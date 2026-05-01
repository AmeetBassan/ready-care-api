package com.readycare.api.repository;

import com.readycare.api.entity.Booking;
import com.readycare.api.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    List<Booking> findAllByOrderByStartTsDesc();

    List<Booking> findByClientIdOrderByStartTsDesc(UUID clientId);

    List<Booking> findByProfessionalIdOrderByStartTsDesc(UUID professionalId);

    List<Booking> findByStatusAndStartTsBefore(BookingStatus status, OffsetDateTime time);

    List<Booking> findByStatusAndStartTsBetweenAndReminderSentAtIsNull(
            BookingStatus status,
            OffsetDateTime fromInclusive,
            OffsetDateTime toExclusive
    );
}
