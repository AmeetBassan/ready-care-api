package com.readycare.api.controller;

import com.readycare.api.dto.AuthLoginRequest;
import com.readycare.api.dto.AuthLoginResponse;
import com.readycare.api.dto.CreateAdminRequest;
import com.readycare.api.dto.CreateClientRequest;
import com.readycare.api.dto.CreateProfessionalRequest;
import com.readycare.api.dto.UserResponse;
import com.readycare.api.entity.UserType;
import com.readycare.api.service.AccountService;
import com.readycare.api.service.AuthService;
import com.readycare.api.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AccountService accountService;
    private final AuthService authService;
    private final NotificationService notificationService;

    public AuthController(AccountService accountService, AuthService authService, NotificationService notificationService) {
        this.accountService = accountService;
        this.authService = authService;
        this.notificationService = notificationService;
    }

    @PostMapping("/register/client")
    public AuthLoginResponse registerClient(@Valid @RequestBody CreateClientRequest request) {
        UserResponse user = accountService.createClient(request);
        notificationService.sendRegistrationConfirmation(user.firstName(), user.email(), user.type());
        return authService.login(new AuthLoginRequest(request.email(), request.password()), UserType.CLIENT);
    }

    @PostMapping("/register/professional")
    public AuthLoginResponse registerProfessional(@Valid @RequestBody CreateProfessionalRequest request) {
        var professional = accountService.createProfessional(request);
        notificationService.sendRegistrationConfirmation(professional.firstName(), professional.email(), UserType.PROFESSIONAL);
        return authService.login(new AuthLoginRequest(request.email(), request.password()), UserType.PROFESSIONAL);
    }

    @PostMapping("/register/admin")
    public AuthLoginResponse registerAdmin(@Valid @RequestBody CreateAdminRequest request) {
        UserResponse user = accountService.createAdmin(request);
        notificationService.sendRegistrationConfirmation(user.firstName(), user.email(), user.type());
        return authService.login(new AuthLoginRequest(request.email(), request.password()), UserType.ADMIN);
    }

    @PostMapping("/login/client")
    public AuthLoginResponse loginClient(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request, UserType.CLIENT);
    }

    @PostMapping("/login/professional")
    public AuthLoginResponse loginProfessional(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request, UserType.PROFESSIONAL);
    }

    @PostMapping("/login/admin")
    public AuthLoginResponse loginAdmin(@Valid @RequestBody AuthLoginRequest request) {
        return authService.login(request, UserType.ADMIN);
    }
}
