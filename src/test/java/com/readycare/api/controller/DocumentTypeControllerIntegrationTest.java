package com.readycare.api.controller;

import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.DocumentTypeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DocumentTypeController.class)
@Import(SecurityConfig.class)
class DocumentTypeControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DocumentTypeService documentTypeService;
    @MockitoBean private JwtDecoder jwtDecoder;

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminJwt() {
        return jwt().jwt(j -> j.subject("77777777-7777-7777-7777-777777777777").claim("userType", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor professionalJwt() {
        return jwt().jwt(j -> j.subject("88888888-8888-8888-8888-888888888888").claim("userType", "PROFESSIONAL"))
                .authorities(new SimpleGrantedAuthority("ROLE_PROFESSIONAL"));
    }

    @Test void getDocumentTypes() throws Exception {
        mockMvc.perform(get("/document-types").with(professionalJwt()))
                .andExpect(status().isOk());
    }

    @Test void createDocumentType() throws Exception {
        mockMvc.perform(post("/document-types")
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Passport\",\"description\":\"desc\",\"required\":true,\"hasExpiry\":true}"))
                .andExpect(status().isOk());
    }

    @Test void patchDocumentType() throws Exception {
        mockMvc.perform(patch("/document-types/{documentTypeId}", UUID.randomUUID())
                        .with(adminJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"required\":false,\"hasExpiry\":true}"))
                .andExpect(status().isOk());
    }
}
