package com.ecommerce.auth.dto;

public record AuthResponse(
    String accessToken,
    String tokenType,
    long expiresInMs,
    UserSummaryDto user
) {
    public static AuthResponse of(String token, long expiresInMs, UserSummaryDto user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
