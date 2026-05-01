package com.readycare.api.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "booking_slots")
public class BookingSlot {

    @EmbeddedId
    private BookingSlotId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("bookingId")
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("availabilitySlotId")
    @JoinColumn(name = "availability_slot_id", nullable = false)
    private AvailabilitySlot availabilitySlot;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public BookingSlotId getId() {
        return id;
    }

    public void setId(BookingSlotId id) {
        this.id = id;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public AvailabilitySlot getAvailabilitySlot() {
        return availabilitySlot;
    }

    public void setAvailabilitySlot(AvailabilitySlot availabilitySlot) {
        this.availabilitySlot = availabilitySlot;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
