package com.readycare.api.repository;

import com.readycare.api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByBooking_Id(UUID bookingId);
}
