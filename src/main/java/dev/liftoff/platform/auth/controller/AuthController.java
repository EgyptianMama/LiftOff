package dev.liftoff.platform.auth.controller;

import dev.liftoff.platform.auth.dto.LoginRequest;
import dev.liftoff.platform.auth.dto.RefreshRequest;
import dev.liftoff.platform.auth.dto.RegisterRequest;
import dev.liftoff.platform.auth.dto.TokenResponse;
import dev.liftoff.platform.auth.dto.UserResponse;
import dev.liftoff.platform.auth.entity.User;
import dev.liftoff.platform.auth.repository.UserRepository;
import dev.liftoff.platform.auth.service.AuthService;
import dev.liftoff.platform.common.exception.LiftoffException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal org.springframework.security.core.userdetails.User principal) {
        
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new LiftoffException("User not found"));
                
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
