package com.readycare.api.controller;

import com.readycare.api.dto.DocumentFileResponse;
import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.BookingService;
import com.readycare.api.service.ReviewService;
import com.readycare.api.service.VerificationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean private VerificationService verificationService;
    @MockitoBean private AccountService accountService;
    @MockitoBean private BookingService bookingService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static final String ADMIN_ID = "11111111-1111-1111-1111-111111111111";

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject(ADMIN_ID).claim("userType", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test void reviewDocument() throws Exception {
        mockMvc.perform(patch("/api/admin/documents/{documentId}/review", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adminId\":\"" + ADMIN_ID + "\",\"approve\":true,\"reason\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test void getPendingProfessionals() throws Exception {
        mockMvc.perform(get("/api/admin/professionals/pending").with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void getUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(adminJwt()).param("userType", "PROFESSIONAL"))
                .andExpect(status().isOk());
    }

    @Test void getUser() throws Exception {
        mockMvc.perform(get("/api/admin/users/{userId}", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void updateUserStatus() throws Exception {
        mockMvc.perform(patch("/api/admin/users/{userId}/status", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test void getProfessionalApplication() throws Exception {
        mockMvc.perform(get("/api/admin/professionals/{professionalId}", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void approveProfessional() throws Exception {
        mockMvc.perform(post("/api/admin/professionals/{professionalId}/approve", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test void rejectProfessional() throws Exception {
        mockMvc.perform(post("/api/admin/professionals/{professionalId}/reject", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no\"}"))
                .andExpect(status().isOk());
    }

    @Test void getBookings() throws Exception {
        mockMvc.perform(get("/api/admin/bookings").with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void getBooking() throws Exception {
        mockMvc.perform(get("/api/admin/bookings/{bookingId}", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void getReviews() throws Exception {
        mockMvc.perform(get("/api/admin/reviews").with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void getDocumentFile() throws Exception {
        when(verificationService.getDocumentFileForAdmin(any(UUID.class)))
                .thenReturn(new DocumentFileResponse("abc".getBytes(), "application/pdf", "doc.pdf"));
        mockMvc.perform(get("/api/admin/documents/{documentId}/file", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }
}
