package com.readycare.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class BookingSlotId implements Serializable {

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "availability_slot_id")
    private UUID availabilitySlotId;

    public BookingSlotId() {
    }

    public BookingSlotId(UUID bookingId, UUID availabilitySlotId) {
        this.bookingId = bookingId;
        this.availabilitySlotId = availabilitySlotId;
    }

    public UUID getBookingId() {
        return bookingId;
    }

    public void setBookingId(UUID bookingId) {
        this.bookingId = bookingId;
    }

    public UUID getAvailabilitySlotId() {
        return availabilitySlotId;
    }

    public void setAvailabilitySlotId(UUID availabilitySlotId) {
        this.availabilitySlotId = availabilitySlotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BookingSlotId that)) {
            return false;
        }
        return Objects.equals(bookingId, that.bookingId)
                && Objects.equals(availabilitySlotId, that.availabilitySlotId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bookingId, availabilitySlotId);
    }
}
