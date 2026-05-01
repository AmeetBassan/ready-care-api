package com.readycare.api.repository;

import com.readycare.api.entity.BookingSlot;
import com.readycare.api.entity.BookingSlotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BookingSlotRepository extends JpaRepository<BookingSlot, BookingSlotId> {
    List<BookingSlot> findByBooking_Id(UUID bookingId);
}
