package com.readycare.api.controller;

import com.readycare.api.dto.UserProfilePictureFileResponse;
import com.readycare.api.dto.UserProfilePictureResponse;
import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@Import(SecurityConfig.class)
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccountService accountService;
    @MockitoBean private ReviewService reviewService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor clientJwt() {
        return jwt().jwt(j -> j.subject("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb").claim("userType", "CLIENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject("cccccccc-cccc-cccc-cccc-cccccccccccc").claim("userType", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test void getMe() throws Exception {
        mockMvc.perform(get("/users/me").with(clientJwt())).andExpect(status().isOk());
    }

    @Test void patchMe() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .with(clientJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"city\":\"London\"}"))
                .andExpect(status().isOk());
    }

    @Test void getMyReviews() throws Exception {
        mockMvc.perform(get("/users/me/reviews").with(clientJwt())).andExpect(status().isOk());
    }

    @Test void uploadProfilePicture() throws Exception {
        when(accountService.uploadProfilePicture(any(UUID.class), any()))
                .thenReturn(new UserProfilePictureResponse("users/test/profile.png"));
        MockMultipartFile file = new MockMultipartFile("file", "profile.png", "image/png", "abc".getBytes());
        mockMvc.perform(multipart("/users/me/profile-picture")
                        .file(file)
                        .with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void getMyProfilePicture() throws Exception {
        when(accountService.getProfilePicture(any(UUID.class)))
                .thenReturn(new UserProfilePictureFileResponse("abc".getBytes(), "image/png", "profile.png"));
        mockMvc.perform(get("/users/me/profile-picture").with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void getUsers() throws Exception {
        mockMvc.perform(get("/users").with(adminJwt()).param("type", "PROFESSIONAL"))
                .andExpect(status().isOk());
    }

    @Test void getUser() throws Exception {
        mockMvc.perform(get("/users/{userId}", UUID.randomUUID()).with(adminJwt())).andExpect(status().isOk());
    }

    @Test void patchUser() throws Exception {
        mockMvc.perform(patch("/users/{userId}", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lastName\":\"Smith\"}"))
                .andExpect(status().isOk());
    }

    @Test void updateUserStatus() throws Exception {
        mockMvc.perform(patch("/users/{userId}/status", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
    }

    @Test void getUserProfilePictureAsAdmin() throws Exception {
        when(accountService.getProfilePicture(any(UUID.class)))
                .thenReturn(new UserProfilePictureFileResponse("abc".getBytes(), "image/png", "profile.png"));
        mockMvc.perform(get("/users/{userId}/profile-picture", UUID.randomUUID()).with(adminJwt()))
                .andExpect(status().isOk());
    }
}
