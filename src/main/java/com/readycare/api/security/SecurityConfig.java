package com.readycare.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/register/admin").hasRole("ADMIN")
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/users/me", "/users/me/**").authenticated()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/document-types").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/document-types").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/document-types/*").hasRole("ADMIN")
                        .requestMatchers("/api/availability/**").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/clients/**").hasAnyRole("CLIENT", "PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/clients/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/clients/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/professionals/*/availability").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/professionals/*/availability/*").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/professionals/*/availability/*").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/professionals/*/documents").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/professionals/*/documents").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/professionals/me/submit-application").hasRole("PROFESSIONAL")
                        .requestMatchers("/api/professionals/me", "/api/professionals/me/**").hasRole("PROFESSIONAL")
                        .requestMatchers(HttpMethod.PUT, "/api/professionals/*").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/professionals/*").hasRole("ADMIN")
                        .requestMatchers("/api/professionals/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/bookings").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/api/bookings/client/**").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers("/api/bookings/professional/**").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/confirm").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/accept").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/reject").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/no-show").hasAnyRole("CLIENT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/complete").hasAnyRole("PROFESSIONAL", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/bookings/*/cancel").hasAnyRole("CLIENT", "PROFESSIONAL", "ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String jwtSecret) {
        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::authoritiesFromUserType);
        return converter;
    }

    private Collection<GrantedAuthority> authoritiesFromUserType(Jwt jwt) {
        String userType = jwt.getClaimAsString("userType");
        if (userType == null || userType.isBlank()) {
            return List.of();
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + userType));
    }
}
