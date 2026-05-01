package com.readycare.api.controller;

import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AccountService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ClientController.class)
@Import(SecurityConfig.class)
class ClientControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AccountService accountService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor clientJwt() {
        return jwt().jwt(j -> j.subject("66666666-6666-6666-6666-666666666666").claim("userType", "CLIENT"))
                .authorities(new SimpleGrantedAuthority("ROLE_CLIENT"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor professionalJwt() {
        return jwt().jwt(j -> j.subject("77777777-7777-7777-7777-777777777777").claim("userType", "PROFESSIONAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
    }

    @Test void getClient() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}", UUID.randomUUID()).with(clientJwt()))
                .andExpect(status().isOk());
    }

    @Test void getClient_asProfessional() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}", UUID.randomUUID()).with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void updateClient() throws Exception {
        mockMvc.perform(put("/api/clients/{clientId}", UUID.randomUUID())
                        .with(clientJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"John\",\"lastName\":\"Doe\",\"phoneNumber\":\"4477\",\"city\":\"London\"}"))
                .andExpect(status().isOk());
    }

    @Test void deleteClient() throws Exception {
        mockMvc.perform(delete("/api/clients/{clientId}", UUID.randomUUID()).with(clientJwt()))
                .andExpect(status().isOk());
    }
}
