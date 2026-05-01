package com.readycare.api.controller;

import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AvailabilityService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AvailabilityController.class)
@Import(SecurityConfig.class)
class AvailabilityControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AvailabilityService availabilityService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor professionalJwt() {
        return jwt().jwt(j -> j.subject("22222222-2222-2222-2222-222222222222").claim("userType", "PROFESSIONAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
    }

    @Test void createAvailability() throws Exception {
        mockMvc.perform(post("/api/availability")
                        .with(professionalJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTs\":\"2026-06-01T10:00:00Z\",\"endTs\":\"2026-06-01T12:00:00Z\",\"status\":\"FREE\"}"))
                .andExpect(status().isOk());
    }

    @Test void getMyAvailability() throws Exception {
        mockMvc.perform(get("/api/availability/me")
                        .with(professionalJwt())
                        .param("from", "2026-06-01T00:00:00Z")
                        .param("to", "2026-06-02T00:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test void updateAvailability() throws Exception {
        mockMvc.perform(patch("/api/availability/{slotId}", UUID.randomUUID())
                        .with(professionalJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startTs\":\"2026-06-01T10:00:00Z\",\"endTs\":\"2026-06-01T11:00:00Z\",\"status\":\"FREE\"}"))
                .andExpect(status().isOk());
    }

    @Test void deleteAvailability() throws Exception {
        mockMvc.perform(delete("/api/availability/{slotId}", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }
}
