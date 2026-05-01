package com.readycare.api.security;

import com.readycare.api.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

// References:
// https://www.springframework.org/spring-security/reference/6.5/servlet/authentication/architecture.html
// https://docs.spring.io/spring-security/reference/6.5/servlet/oauth2/resource-server/jwt.html
// https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JsonMapper jsonMapper;
    private final String jwtSecret;
    private final long jwtExpirationMs;

    public JwtTokenProvider(
            JsonMapper jsonMapper,
            @Value("${app.jwt.secret}") String jwtSecret,
            @Value("${app.jwt.expiration}") long jwtExpirationMs
    ) {
        this.jsonMapper = jsonMapper;
        this.jwtSecret = jwtSecret;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(User user) {
        long issuedAtSeconds = Instant.now().getEpochSecond();
        long expiresAtSeconds = issuedAtSeconds + expiresInSeconds();

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getId().toString());
        payload.put("userId", user.getId().toString());
        payload.put("email", user.getEmail());
        payload.put("userType", user.getType().name());
        payload.put("iat", issuedAtSeconds);
        payload.put("exp", expiresAtSeconds);

        String headerBase64 = base64UrlEncode(toJson(header));
        String payloadBase64 = base64UrlEncode(toJson(payload));
        String signingInput = headerBase64 + "." + payloadBase64;
        return signingInput + "." + base64UrlEncode(sign(signingInput));
    }

    public long expiresInSeconds() {
        return jwtExpirationMs / 1000;
    }

    private String toJson(Map<String, Object> value) {
        try {
            return jsonMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize JWT payload", e);
        }
    }

    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to sign JWT", e);
        }
    }

    private String base64UrlEncode(String value) {
        return base64UrlEncode(value.getBytes(StandardCharsets.UTF_8));
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
