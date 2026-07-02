package com.nihongolisten.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        Long userId,
        String email,
        String role
) {
}
