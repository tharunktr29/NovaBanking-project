package com.novabank.auth.security;

import com.novabank.auth.config.JwtProperties;
import com.novabank.auth.domain.UserCredential;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenResult createAccessToken(UserCredential user) {
        var now = Instant.now();
        var expiresAt = now.plus(properties.accessTokenTtl());
        var token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(user.getId().toString())
                .claims(Map.of(
                        "username", user.getUsername(),
                        "role", "ROLE_" + user.getRole().name()
                ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
        return new TokenResult(token, expiresAt);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record TokenResult(String token, Instant expiresAt) {
    }
}
