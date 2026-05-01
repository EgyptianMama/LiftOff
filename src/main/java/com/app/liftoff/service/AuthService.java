package com.app.liftoff.service;

import com.app.liftoff.dto.auth.AuthResponse;
import com.app.liftoff.dto.auth.LoginRequest;
import com.app.liftoff.dto.auth.RegisterRequest;
import com.app.liftoff.entity.RefreshToken;
import com.app.liftoff.entity.User;
import com.app.liftoff.repository.RefreshTokenRepository;
import com.app.liftoff.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with this email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenStr = jwtService.generateRefreshToken(user.getEmail());

        saveRefreshToken(user, refreshTokenStr);

        return new AuthResponse(accessToken, refreshTokenStr);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getEmail());
        String refreshTokenStr = jwtService.generateRefreshToken(user.getEmail());

        saveRefreshToken(user, refreshTokenStr);

        return new AuthResponse(accessToken, refreshTokenStr);
    }

    public AuthResponse refreshToken(String refreshToken) {
        String email = jwtService.extractEmail(refreshToken);

        if (!jwtService.isTokenValid(refreshToken, email)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        String newAccessToken = jwtService.generateAccessToken(email);
        String newRefreshToken = jwtService.generateRefreshToken(email);

        // Optional: Revoke old refresh token here in future

        return new AuthResponse(newAccessToken, newRefreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokenRepository.findByTokenHash(refreshToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private void saveRefreshToken(User user, String refreshTokenStr) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenStr)                    // TODO: Hash this in production
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}