package com.readycare.api.controller;

import com.readycare.api.dto.AuthLoginResponse;
import com.readycare.api.dto.CreateAdminRequest;
import com.readycare.api.dto.CreateClientRequest;
import com.readycare.api.dto.CreateProfessionalRequest;
import com.readycare.api.dto.ProfessionalResponse;
import com.readycare.api.dto.UserResponse;
import com.readycare.api.entity.UserType;
import com.readycare.api.entity.VerificationStatus;
import com.readycare.api.security.SecurityConfig;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.AuthService;
import com.readycare.api.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerRegisterAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final String REQUEST_JSON = """
            {
              "firstName": "New",
              "lastName": "Admin",
              "gender": "MALE",
              "dob": "1991-02-15",
              "email": "new.admin@example.com",
              "phoneNumber": "447700000002",
              "password": "admin123",
              "primaryAddress": {
                "label": "Home",
                "line1": "10 Admin Street",
                "line2": "",
                "city": "London",
                "postcode": "N1 1AA",
                "country": "GB"
              }
            }
            """;
    private static final String CLIENT_REGISTER_JSON = """
            {
              "firstName": "Client",
              "lastName": "User",
              "gender": "MALE",
              "dob": "1992-01-01",
              "email": "client@example.com",
              "phoneNumber": "447700000003",
              "password": "client123",
              "primaryAddress": {
                "label": "Home",
                "line1": "1 Client Street",
                "line2": "",
                "city": "London",
                "postcode": "E1 1AA",
                "country": "GB"
              }
            }
            """;
    private static final String PROFESSIONAL_REGISTER_JSON = """
            {
              "firstName": "Pro",
              "lastName": "User",
              "gender": "FEMALE",
              "dob": "1990-05-05",
              "email": "professional@example.com",
              "phoneNumber": "447700000004",
              "password": "pro123",
              "bio": "bio",
              "yearsExperience": 4,
              "hourlyRateOfficeHours": 25.0,
              "hourlyRateOutOfOfficeHours": 30.0,
              "primaryAddress": {
                "label": "Home",
                "line1": "1 Pro Street",
                "line2": "",
                "city": "London",
                "postcode": "E1 1AB",
                "country": "GB"
              }
            }
            """;

    @Test
    void registerAdmin_withoutJwt_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isUnauthorized());

        verify(accountService, never()).createAdmin(any(CreateAdminRequest.class));
    }

    @Test
    void registerAdmin_withNonAdminJwt_returnsForbidden() throws Exception {
        mockMvc.perform(post("/auth/register/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CLIENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isForbidden());

        verify(accountService, never()).createAdmin(any(CreateAdminRequest.class));
    }

    @Test
    void registerAdmin_withAdminJwt_createsAdminAndReturnsToken() throws Exception {
        when(accountService.createAdmin(any(CreateAdminRequest.class))).thenReturn(new UserResponse(
                UUID.randomUUID(),
                UserType.ADMIN,
                "New",
                "Admin",
                "new.admin@example.com",
                "447700000002",
                UUID.randomUUID(),
                "London"
        ));
        when(authService.login(any(), eq(UserType.ADMIN))).thenReturn(new AuthLoginResponse(
                "jwt-token",
                "Bearer",
                86400L,
                "d290f1ee-6c54-4b01-90e6-d701748f0851",
                "new.admin@example.com",
                "ADMIN",
                "Login successful"
        ));

        mockMvc.perform(post("/auth/register/admin")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt-token"))
                .andExpect(jsonPath("$.user_type").value("ADMIN"));

        verify(accountService).createAdmin(any(CreateAdminRequest.class));
        verify(authService).login(any(), eq(UserType.ADMIN));
    }

    @Test
    void registerClient_publicEndpoint_returnsToken() throws Exception {
        when(accountService.createClient(any(CreateClientRequest.class))).thenReturn(new UserResponse(
                UUID.randomUUID(),
                UserType.CLIENT,
                "Client",
                "User",
                "client@example.com",
                "447700000003",
                UUID.randomUUID(),
                "London"
        ));
        when(authService.login(any(), eq(UserType.CLIENT))).thenReturn(new AuthLoginResponse(
                "client-token", "Bearer", 86400L, "id", "client@example.com", "CLIENT", "ok"
        ));

        mockMvc.perform(post("/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLIENT_REGISTER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("client-token"))
                .andExpect(jsonPath("$.user_type").value("CLIENT"));

        verify(accountService).createClient(any(CreateClientRequest.class));
    }

    @Test
    void registerProfessional_publicEndpoint_returnsToken() throws Exception {
        when(accountService.createProfessional(any(CreateProfessionalRequest.class))).thenReturn(new ProfessionalResponse(
                UUID.randomUUID(),
                "Pro",
                "User",
                "professional@example.com",
                "447700000004",
                UUID.randomUUID(),
                "London",
                "bio",
                4,
                BigDecimal.valueOf(25.0),
                BigDecimal.valueOf(30.0),
                VerificationStatus.NOT_SUBMITTED
        ));
        when(authService.login(any(), eq(UserType.PROFESSIONAL))).thenReturn(new AuthLoginResponse(
                "pro-token", "Bearer", 86400L, "id", "professional@example.com", "PROFESSIONAL", "ok"
        ));

        mockMvc.perform(post("/auth/register/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PROFESSIONAL_REGISTER_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("pro-token"))
                .andExpect(jsonPath("$.user_type").value("PROFESSIONAL"));

        verify(accountService).createProfessional(any(CreateProfessionalRequest.class));
    }

    @Test
    void loginClient_publicEndpoint() throws Exception {
        when(authService.login(any(), eq(UserType.CLIENT))).thenReturn(new AuthLoginResponse(
                "client-token", "Bearer", 86400L, "id", "client@example.com", "CLIENT", "ok"
        ));

        mockMvc.perform(post("/auth/login/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"client@example.com\",\"password\":\"client123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("client-token"));
    }

    @Test
    void loginProfessional_publicEndpoint() throws Exception {
        when(authService.login(any(), eq(UserType.PROFESSIONAL))).thenReturn(new AuthLoginResponse(
                "pro-token", "Bearer", 86400L, "id", "professional@example.com", "PROFESSIONAL", "ok"
        ));

        mockMvc.perform(post("/auth/login/professional")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"professional@example.com\",\"password\":\"pro123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("pro-token"));
    }

    @Test
    void loginAdmin_publicEndpoint() throws Exception {
        when(authService.login(any(), eq(UserType.ADMIN))).thenReturn(new AuthLoginResponse(
                "admin-token", "Bearer", 86400L, "id", "admin@example.com", "ADMIN", "ok"
        ));

        mockMvc.perform(post("/auth/login/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@example.com\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("admin-token"));
    }

}
