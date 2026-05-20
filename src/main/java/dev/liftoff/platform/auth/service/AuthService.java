package dev.liftoff.platform.auth.service;

import dev.liftoff.platform.auth.dto.LoginRequest;
import dev.liftoff.platform.auth.dto.RegisterRequest;
import dev.liftoff.platform.auth.dto.TokenResponse;
import dev.liftoff.platform.auth.entity.User;
import dev.liftoff.platform.auth.repository.UserRepository;
import dev.liftoff.platform.common.exception.LiftoffException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new LiftoffException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new LiftoffException("Username is already taken");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("USER"); // Default role

        userRepository.save(user);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user, null);

        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.createRefreshToken(user, null);

        return new TokenResponse(accessToken, refreshToken);
    }

    public TokenResponse refresh(String refreshToken) {
        String[] tokens = jwtService.rotateRefreshToken(refreshToken);
        return new TokenResponse(tokens[0], tokens[1]);
    }

    public void logout(String refreshToken) {
        jwtService.revokeRefreshToken(refreshToken);
    }
}
