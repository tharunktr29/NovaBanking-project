package com.novabank.auth.service;

import com.novabank.auth.config.JwtProperties;
import com.novabank.auth.domain.RefreshToken;
import com.novabank.auth.domain.UserCredential;
import com.novabank.auth.exception.AuthException;
import com.novabank.auth.repository.RefreshTokenRepository;
import com.novabank.auth.security.TokenHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    public IssuedRefreshToken issue(UserCredential user) {
        var rawToken = randomToken();
        var refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHasher.sha256Base64Url(rawToken));
        refreshToken.setExpiresAt(Instant.now().plus(jwtProperties.refreshTokenTtl()));
        refreshTokenRepository.save(refreshToken);
        return new IssuedRefreshToken(rawToken, refreshToken);
    }

    public RefreshToken requireActive(String rawToken) {
        var hash = TokenHasher.sha256Base64Url(rawToken);
        var token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> invalidRefreshToken());
        if (token.getRevokedAt() != null || token.getExpiresAt().isBefore(Instant.now())) {
            throw invalidRefreshToken();
        }
        return token;
    }

    public void revoke(String rawToken) {
        var hash = TokenHasher.sha256Base64Url(rawToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.revoke(null);
                refreshTokenRepository.save(token);
            }
        });
    }

    public void revokeAllForUser(UserCredential user) {
        var tokens = refreshTokenRepository.findByUser_IdAndRevokedAtIsNull(user.getId());
        tokens.forEach(token -> token.revoke(null));
        refreshTokenRepository.saveAll(tokens);
    }

    public void rotate(RefreshToken oldToken, RefreshToken newToken) {
        oldToken.revoke(newToken.getTokenHash());
        refreshTokenRepository.save(oldToken);
    }

    private String randomToken() {
        var bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthException invalidRefreshToken() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    public record IssuedRefreshToken(String rawToken, RefreshToken entity) {
    }
}
