package com.novabank.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "login_attempts", indexes = @Index(name = "idx_login_attempts_username_time", columnList = "username_or_email, attempted_at"))
public class LoginAttempt {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "username_or_email", nullable = false, length = 160)
    private String usernameOrEmail;

    @Column(nullable = false)
    private boolean successful;

    @Column(name = "failure_reason", length = 80)
    private String failureReason;

    @Column(name = "ip_address", length = 80)
    private String ipAddress;

    @Column(name = "user_agent", length = 240)
    private String userAgent;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();

    protected LoginAttempt() {
    }

    public LoginAttempt(String usernameOrEmail, boolean successful, String failureReason, String ipAddress) {
        this.usernameOrEmail = usernameOrEmail;
        this.successful = successful;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
    }

    public LoginAttempt(String usernameOrEmail, boolean successful, String failureReason, String ipAddress, String userAgent) {
        this.usernameOrEmail = usernameOrEmail;
        this.successful = successful;
        this.failureReason = failureReason;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public String getUsernameOrEmail() { return usernameOrEmail; }
    public boolean isSuccessful() { return successful; }
    public String getFailureReason() { return failureReason; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public Instant getAttemptedAt() { return attemptedAt; }
}
