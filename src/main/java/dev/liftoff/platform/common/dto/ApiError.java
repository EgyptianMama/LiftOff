package dev.liftoff.platform.common.dto;

public record ApiError(
        int status,
        String error,
        String message
) {}
