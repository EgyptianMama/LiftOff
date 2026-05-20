package dev.liftoff.platform.auth.service;

import dev.liftoff.platform.auth.entity.RefreshToken;
import dev.liftoff.platform.auth.entity.User;
import dev.liftoff.platform.auth.repository.RefreshTokenRepository;
import dev.liftoff.platform.common.exception.LiftoffException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${liftoff.jwt.secret}")
    private String secret;

    @Value("${liftoff.jwt.access-token-expiry}")
    private Duration accessTokenExpiry;

    @Value("${liftoff.jwt.refresh-token-expiry}")
    private Duration refreshTokenExpiry;

    private SecretKey key;

    private final RefreshTokenRepository refreshTokenRepository;

    @PostConstruct
    public void init() {
        try {
            // We hash the incoming string to guarantee it's exactly 256 bits for HMAC-SHA256
            byte[] keyBytes = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            this.key = Keys.hmacShaKeyFor(keyBytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize JwtService", e);
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiry);

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        String role = claims.get("role", String.class);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        
        org.springframework.security.core.userdetails.User principal = 
            new org.springframework.security.core.userdetails.User(claims.getSubject(), "", authorities);
            
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    @Transactional
    public String createRefreshToken(User user, String familyId) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setTokenFamily(familyId != null ? familyId : UUID.randomUUID().toString());
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenExpiry));

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public String[] rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        
        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new LiftoffException("Invalid refresh token"));

        if (existingToken.isRevoked() || existingToken.getExpiresAt().isBefore(Instant.now())) {
            // Token theft detected! Someone is trying to use an old/revoked token.
            // Revoke the ENTIRE token family.
            refreshTokenRepository.revokeFamily(existingToken.getTokenFamily());
            throw new LiftoffException("Refresh token is expired or revoked. Please log in again.");
        }

        // Revoke the old token
        existingToken.setRevoked(true);
        refreshTokenRepository.save(existingToken);

        // Generate a new token pair using the same family ID
        User user = existingToken.getUser();
        String newAccessToken = generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user, existingToken.getTokenFamily());

        return new String[]{newAccessToken, newRefreshToken};
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
