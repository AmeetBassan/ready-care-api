package com.readycare.api.controller;

import com.readycare.api.dto.DocumentFileResponse;
import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.AvailabilityService;
import com.readycare.api.service.ReviewService;
import com.readycare.api.service.SearchService;
import com.readycare.api.service.VerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfessionalController.class)
@Import(SecurityConfig.class)
class ProfessionalControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccountService accountService;
    @MockitoBean private AvailabilityService availabilityService;
    @MockitoBean private VerificationService verificationService;
    @MockitoBean private SearchService searchService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor professionalJwt() {
        return jwt().jwt(j -> j.subject("99999999-9999-9999-9999-999999999999").claim("userType", "PROFESSIONAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa").claim("userType", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test void getProfessionals() throws Exception {
        mockMvc.perform(get("/api/professionals").with(professionalJwt())).andExpect(status().isOk());
    }

    @Test void getProfessional() throws Exception {
        mockMvc.perform(get("/api/professionals/{professionalId}", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void getMe() throws Exception {
        mockMvc.perform(get("/api/professionals/me").with(professionalJwt())).andExpect(status().isOk());
    }

    @Test void getProfessionalAddress() throws Exception {
        mockMvc.perform(get("/api/professionals/{professionalId}/address", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void updateProfessional() throws Exception {
        mockMvc.perform(put("/api/professionals/{professionalId}", UUID.randomUUID())
                        .with(professionalJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"phoneNumber\":\"447\",\"city\":\"London\"}"))
                .andExpect(status().isOk());
    }

    @Test void updateMe() throws Exception {
        mockMvc.perform(patch("/api/professionals/me")
                        .with(professionalJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"updated\",\"yearsExperience\":4}"))
                .andExpect(status().isOk());
    }

    @Test void deleteProfessional() throws Exception {
        mockMvc.perform(delete("/api/professionals/{professionalId}", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }

    @Test void upsertAvailability() throws Exception {
        mockMvc.perform(put("/api/professionals/{professionalId}/availability", UUID.randomUUID())
                        .with(professionalJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"day\":\"2026-06-10\",\"startHour\":9,\"endHour\":17,\"status\":\"FREE\"}"))
                .andExpect(status().isOk());
    }

    @Test void listAvailability() throws Exception {
        mockMvc.perform(get("/api/professionals/{professionalId}/availability", UUID.randomUUID())
                        .with(professionalJwt())
                        .param("from", "2026-06-10T00:00:00Z")
                        .param("to", "2026-06-11T00:00:00Z"))
                .andExpect(status().isOk());
    }

    @Test void setSlotStatus() throws Exception {
        mockMvc.perform(patch("/api/professionals/{professionalId}/availability/{slotId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(professionalJwt())
                        .param("status", "BLOCKED"))
                .andExpect(status().isOk());
    }

    @Test void deleteAvailabilitySlot() throws Exception {
        mockMvc.perform(delete("/api/professionals/{professionalId}/availability/{slotId}", UUID.randomUUID(), UUID.randomUUID())
                        .with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void submitApplication() throws Exception {
        mockMvc.perform(post("/api/professionals/me/submit-application").with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void uploadMyDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "abc".getBytes());
        MockPart documentTypeId = new MockPart("documentTypeId", "\"11111111-1111-1111-1111-111111111111\"".getBytes());
        documentTypeId.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MockPart expiryDate = new MockPart("expiryDate", "\"2027-01-01\"".getBytes());
        expiryDate.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(multipart("/api/professionals/me/documents")
                        .file(file)
                        .part(documentTypeId)
                        .part(expiryDate)
                        .with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void getMyDocuments() throws Exception {
        mockMvc.perform(get("/api/professionals/me/documents").with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void getMyDocumentFile() throws Exception {
        when(verificationService.getDocumentFileForProfessional(any(UUID.class), any(UUID.class)))
                .thenReturn(new DocumentFileResponse("abc".getBytes(), "application/pdf", "doc.pdf"));
        mockMvc.perform(get("/api/professionals/me/documents/{documentId}/file", UUID.randomUUID())
                        .with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void uploadDocumentById() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "abc".getBytes());
        MockPart documentTypeId = new MockPart("documentTypeId", "\"11111111-1111-1111-1111-111111111111\"".getBytes());
        documentTypeId.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(multipart("/api/professionals/{professionalId}/documents", UUID.randomUUID())
                        .file(file)
                        .part(documentTypeId)
                        .with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void getDocumentsById() throws Exception {
        mockMvc.perform(get("/api/professionals/{professionalId}/documents", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void search() throws Exception {
        mockMvc.perform(get("/api/professionals/search")
                        .with(professionalJwt())
                        .param("city", "London")
                        .param("startTs", "2026-06-10T10:00:00Z")
                        .param("durationHours", "2"))
                .andExpect(status().isOk());
    }

    @Test void getProfessionalReviews() throws Exception {
        mockMvc.perform(get("/api/professionals/{professionalId}/reviews", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }
}
