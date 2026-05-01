package com.readycare.api.service;

import com.readycare.api.entity.Booking;
import com.readycare.api.entity.Payment;
import com.readycare.api.entity.PaymentStatus;
import com.readycare.api.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment createMockPaidPayment(Booking booking, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setProvider("MOCK");
        payment.setProviderReference("MOCK-" + booking.getId());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(OffsetDateTime.now());
        return paymentRepository.save(payment);
    }

    @Transactional
    public void refundFullIfPaid(Booking booking, String reason) {
        paymentRepository.findByBooking_Id(booking.getId()).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID || payment.getStatus() == PaymentStatus.PARTIALLY_REFUNDED) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(payment.getAmount());
                payment.setRefundedAt(OffsetDateTime.now());
                payment.setRefundReason(reason);
                paymentRepository.save(payment);
            }
        });
    }

    @Transactional
    public void doNotRefund(Booking booking, String reason) {
        paymentRepository.findByBooking_Id(booking.getId()).ifPresent(payment -> {
            payment.setRefundReason(reason);
            paymentRepository.save(payment);
        });
    }

    public Payment findByBookingId(Booking booking) {
        return paymentRepository.findByBooking_Id(booking.getId()).orElse(null);
    }
}
