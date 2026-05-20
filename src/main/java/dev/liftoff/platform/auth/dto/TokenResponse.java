package dev.liftoff.platform.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
