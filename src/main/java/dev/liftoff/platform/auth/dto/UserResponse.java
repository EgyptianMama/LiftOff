package dev.liftoff.platform.auth.dto;

import dev.liftoff.platform.auth.entity.User;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String username,
        String role
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole()
        );
    }
}
