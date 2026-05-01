package com.readycare.api.service;

import com.readycare.api.dto.AuthLoginRequest;
import com.readycare.api.dto.AuthLoginResponse;
import com.readycare.api.entity.User;
import com.readycare.api.entity.UserType;
import com.readycare.api.exception.UnauthorizedException;
import com.readycare.api.repository.UserRepository;
import com.readycare.api.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final HashService hashService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(
            UserRepository userRepository,
            HashService hashService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.hashService = hashService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public AuthLoginResponse login(AuthLoginRequest request, UserType expectedType) {
        User user = userRepository.findByEmail(request.email())
                .filter(foundUser -> foundUser.getType() == expectedType)
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE));

        String passwordHash = hashService.sha256(request.password());
        if (!passwordHash.equals(user.getPasswordHash())) {
            throw new UnauthorizedException(INVALID_CREDENTIALS_MESSAGE);
        }

        return new AuthLoginResponse(
                jwtTokenProvider.generateToken(user),
                "Bearer",
                jwtTokenProvider.expiresInSeconds(),
                user.getId().toString(),
                user.getEmail(),
                user.getType().name(),
                "Login successful"
        );
    }
}
