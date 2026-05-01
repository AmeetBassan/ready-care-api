package com.readycare.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuthLoginResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("user_id")
        String userId,

        @JsonProperty("email")
        String email,

        @JsonProperty("user_type")
        String userType,

        String message
) {}
