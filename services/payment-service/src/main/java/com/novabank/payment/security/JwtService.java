package com.novabank.payment.security;

import com.novabank.payment.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(signingKey).requireIssuer(properties.issuer()).build().parseSignedClaims(token).getPayload();
    }

    public String createInternalAccessToken(UUID customerId) {
        var now = Instant.now();
        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(customerId.toString())
                .claims(Map.of("role", "ROLE_CUSTOMER", "internal", true))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
