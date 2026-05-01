package com.readycare.api.controller;

import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.BookingService;
import com.readycare.api.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BookingController.class)
@Import(SecurityConfig.class)
class BookingControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor clientJwt() {
        return jwt().jwt(j -> j.subject("33333333-3333-3333-3333-333333333333").claim("userType", "CLIENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor professionalJwt() {
        return jwt().jwt(j -> j.subject("44444444-4444-4444-4444-444444444444").claim("userType", "PROFESSIONAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
    }

    @Test void createBooking() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .with(clientJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":\"33333333-3333-3333-3333-333333333333\",\"professionalId\":\"44444444-4444-4444-4444-444444444444\",\"addressId\":\"55555555-5555-5555-5555-555555555555\",\"startTs\":\"2026-06-02T10:00:00Z\",\"endTs\":\"2026-06-02T12:00:00Z\",\"clientNotes\":\"note\"}"))
                .andExpect(status().isOk());
    }

    @Test void getClientBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/client/{clientId}", UUID.randomUUID()).with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void getProfessionalBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/professional/{professionalId}", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void getMyBookings() throws Exception {
        mockMvc.perform(get("/api/bookings/me").with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void getBooking() throws Exception {
        mockMvc.perform(get("/api/bookings/{bookingId}", UUID.randomUUID()).with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void confirmBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/confirm", UUID.randomUUID())
                        .with(professionalJwt())
                        .param("professionalId", "44444444-4444-4444-4444-444444444444"))
                .andExpect(status().isOk());
    }

    @Test void acceptBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/accept", UUID.randomUUID())
                        .with(professionalJwt())
                        .param("professionalId", "44444444-4444-4444-4444-444444444444"))
                .andExpect(status().isOk());
    }

    @Test void rejectBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/reject", UUID.randomUUID())
                        .with(professionalJwt())
                        .param("professionalId", "44444444-4444-4444-4444-444444444444")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"busy\"}"))
                .andExpect(status().isOk());
    }

    @Test void cancelBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/cancel", UUID.randomUUID())
                        .with(clientJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actorUserId\":\"33333333-3333-3333-3333-333333333333\",\"cancelledBy\":\"CLIENT\"}"))
                .andExpect(status().isOk());
    }

    @Test void markNoShow() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/no-show", UUID.randomUUID())
                        .with(clientJwt())
                        .param("clientId", "33333333-3333-3333-3333-333333333333"))
                .andExpect(status().isOk());
    }

    @Test void completeBooking() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/complete", UUID.randomUUID())
                        .with(professionalJwt())
                        .param("professionalId", "44444444-4444-4444-4444-444444444444"))
                .andExpect(status().isOk());
    }

    @Test void createReview() throws Exception {
        mockMvc.perform(post("/api/bookings/{bookingId}/reviews", UUID.randomUUID())
                        .with(clientJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"great\"}"))
                .andExpect(status().isOk());
    }

    @Test void getBookingReviews() throws Exception {
        mockMvc.perform(get("/api/bookings/{bookingId}/reviews", UUID.randomUUID()).with(clientJwt()))
                .andExpect(status().isOk());
    }
}
