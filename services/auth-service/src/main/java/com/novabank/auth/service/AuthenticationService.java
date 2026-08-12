package com.novabank.auth.service;

import com.novabank.auth.domain.LoginAttempt;
import com.novabank.auth.domain.PasswordResetToken;
import com.novabank.auth.domain.UserCredential;
import com.novabank.auth.domain.UserRole;
import com.novabank.auth.exception.AuthException;
import com.novabank.auth.repository.LoginAttemptRepository;
import com.novabank.auth.repository.PasswordResetTokenRepository;
import com.novabank.auth.repository.UserCredentialRepository;
import com.novabank.auth.security.JwtService;
import com.novabank.auth.security.TokenHasher;
import com.novabank.auth.web.dto.*;
import com.novabank.shared.events.BankingEvent;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthenticationService {
    private static final String GENERIC_LOGIN_MESSAGE = "Invalid username/email or password";
    private static final int MAX_FAILED_LOGINS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    private final UserCredentialRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final BankingEventPublisher eventPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthenticationService(
            UserCredentialRepository userRepository,
            LoginAttemptRepository loginAttemptRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuditService auditService,
            BankingEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.loginAttemptRepository = loginAttemptRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String correlationId) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new AuthException(HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS", "Username is already registered");
        }
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new AuthException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered");
        }

        var user = new UserCredential();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.CUSTOMER);
        userRepository.save(user);
        auditService.record(user.getId(), "CUSTOMER_REGISTERED", correlationId, "demo registration");
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent, String correlationId) {
        var user = userRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(request.usernameOrEmail(), request.usernameOrEmail())
                .orElse(null);

        if (user == null) {
            loginAttemptRepository.save(new LoginAttempt(request.usernameOrEmail(), false, "NOT_FOUND_OR_BAD_PASSWORD", ipAddress, safeUserAgent(userAgent)));
            throw invalidLogin();
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            loginAttemptRepository.save(new LoginAttempt(user.getUsername(), false, "LOCKED", ipAddress, safeUserAgent(userAgent)));
            throw new AuthException(HttpStatus.LOCKED, "ACCOUNT_TEMPORARILY_LOCKED", "Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            var failures = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failures);
            if (failures >= MAX_FAILED_LOGINS) {
                user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
                eventPublisher.publish("suspicious-login-detected", BankingEvent.v1(
                        "SuspiciousLoginDetected",
                        correlationId,
                        user.getId(),
                        user.getId(),
                        Map.of("reason", "too_many_failed_attempts")
                ));
            }
            loginAttemptRepository.save(new LoginAttempt(user.getUsername(), false, "NOT_FOUND_OR_BAD_PASSWORD", ipAddress, safeUserAgent(userAgent)));
            throw invalidLogin();
        }

        user.resetFailedLogins();
        loginAttemptRepository.save(new LoginAttempt(user.getUsername(), true, null, ipAddress, safeUserAgent(userAgent)));
        auditService.record(user.getId(), "CUSTOMER_LOGGED_IN", correlationId, "password login");
        eventPublisher.publish("customer.logged-in", BankingEvent.v1(
                "CustomerLoggedIn",
                correlationId,
                user.getId(),
                user.getId(),
                Map.of("username", user.getUsername())
        ));
        return issueAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        var oldToken = refreshTokenService.requireActive(request.refreshToken());
        var user = oldToken.getUser();
        var newRefreshToken = refreshTokenService.issue(user);
        refreshTokenService.rotate(oldToken, newRefreshToken.entity());
        var accessToken = jwtService.createAccessToken(user);
        return new AuthResponse(
                accessToken.token(),
                newRefreshToken.rawToken(),
                "Bearer",
                accessToken.expiresAt(),
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    @Transactional
    public void logout(LogoutRequest request, String correlationId) {
        var token = refreshTokenService.requireActive(request.refreshToken());
        refreshTokenService.revoke(request.refreshToken());
        auditService.record(token.getUser().getId(), "CUSTOMER_LOGGED_OUT", correlationId, "refresh token revoked");
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request, String correlationId) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRMATION_MISMATCH", "New password and confirmation must match");
        }
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "CURRENT_PASSWORD_INVALID", "Current password is invalid");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "PASSWORD_REUSE_REJECTED", "New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLogins();
        refreshTokenService.revokeAllForUser(user);
        auditService.record(user.getId(), "PASSWORD_CHANGED", correlationId, "all refresh tokens revoked");
    }

    @Transactional(readOnly = true)
    public List<LoginActivityResponse> loginActivity(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found"));
        return loginAttemptRepository.findRecentByUsernameOrEmail(
                        List.of(user.getUsername(), user.getEmail()),
                        PageRequest.of(0, 25))
                .stream()
                .map(this::toLoginActivity)
                .toList();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request, String correlationId) {
        userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(request.email(), request.email()).ifPresent(user -> {
            var rawToken = randomToken();
            var resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(TokenHasher.sha256Base64Url(rawToken));
            resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
            passwordResetTokenRepository.save(resetToken);
            auditService.record(user.getId(), "PASSWORD_RESET_REQUESTED", correlationId, "mock reset token created");
            eventPublisher.publish("notification.requested", BankingEvent.v1(
                    "NotificationRequested",
                    correlationId,
                    user.getId(),
                    user.getId(),
                    Map.of("channel", "email", "template", "password_reset_mock")
            ));
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request, String correlationId) {
        var resetToken = passwordResetTokenRepository
                .findByTokenHash(TokenHasher.sha256Base64Url(request.resetToken()))
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Reset token is invalid or expired"));
        if (!resetToken.isUsable(Instant.now())) {
            throw new AuthException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Reset token is invalid or expired");
        }
        var user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.resetFailedLogins();
        resetToken.markUsed();
        auditService.record(user.getId(), "PASSWORD_RESET_COMPLETED", correlationId, "password changed");
    }

    public void verifyMockMfa(MfaVerifyRequest request) {
        if (!"000000".equals(request.code())) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_MFA_CODE", "MFA code is invalid");
        }
    }

    private LoginActivityResponse toLoginActivity(LoginAttempt attempt) {
        var eventType = attempt.isSuccessful() ? "LOGIN_SUCCESS" : "LOGIN_FAILURE";
        return new LoginActivityResponse(
                attempt.getAttemptedAt(),
                attempt.isSuccessful(),
                eventType,
                maskIp(attempt.getIpAddress()),
                summarizeDevice(attempt.getUserAgent())
        );
    }

    private String maskIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }
        if (ipAddress.contains(":")) {
            var parts = ipAddress.split(":");
            return parts.length == 0 ? "masked" : parts[0] + ":****";
        }
        var parts = ipAddress.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return "masked";
    }

    private String summarizeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        var lower = userAgent.toLowerCase();
        var browser = lower.contains("edg") ? "Edge"
                : lower.contains("chrome") ? "Chrome"
                : lower.contains("firefox") ? "Firefox"
                : lower.contains("safari") ? "Safari"
                : "Browser";
        var os = lower.contains("windows") ? "Windows"
                : lower.contains("mac") ? "macOS"
                : lower.contains("linux") ? "Linux"
                : lower.contains("android") ? "Android"
                : lower.contains("iphone") || lower.contains("ipad") ? "iOS"
                : "Unknown OS";
        return browser + " on " + os;
    }

    private String safeUserAgent(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        return userAgent.length() <= 240 ? userAgent : userAgent.substring(0, 240);
    }

    private AuthResponse issueAuthResponse(UserCredential user) {
        var accessToken = jwtService.createAccessToken(user);
        var refreshToken = refreshTokenService.issue(user);
        return new AuthResponse(
                accessToken.token(),
                refreshToken.rawToken(),
                "Bearer",
                accessToken.expiresAt(),
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    private AuthException invalidLogin() {
        return new AuthException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", GENERIC_LOGIN_MESSAGE);
    }

    private String randomToken() {
        var bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
