package com.novabank.customer.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_preferences", indexes = @Index(name = "idx_customer_preferences_customer_id", columnList = "customer_id", unique = true))
public class CustomerPreference {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "customer_id", nullable = false, unique = true)
    private UUID customerId;

    @Column(name = "email_alerts", nullable = false)
    private boolean emailAlerts = true;

    @Column(name = "sms_alerts", nullable = false)
    private boolean smsAlerts;

    @Column(name = "push_alerts", nullable = false)
    private boolean pushAlerts = true;

    @Column(name = "security_alerts", nullable = false)
    private boolean securityAlerts = true;

    @Column(name = "payment_alerts", nullable = false)
    private boolean paymentAlerts = true;

    @Column(name = "low_balance_alerts", nullable = false)
    private boolean lowBalanceAlerts = true;

    @Column(name = "paperless_statements", nullable = false)
    private boolean paperlessStatements = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public boolean isEmailAlerts() { return emailAlerts; }
    public void setEmailAlerts(boolean emailAlerts) { this.emailAlerts = emailAlerts; }
    public boolean isSmsAlerts() { return smsAlerts; }
    public void setSmsAlerts(boolean smsAlerts) { this.smsAlerts = smsAlerts; }
    public boolean isPushAlerts() { return pushAlerts; }
    public void setPushAlerts(boolean pushAlerts) { this.pushAlerts = pushAlerts; }
    public boolean isSecurityAlerts() { return securityAlerts; }
    public void setSecurityAlerts(boolean securityAlerts) { this.securityAlerts = securityAlerts; }
    public boolean isPaymentAlerts() { return paymentAlerts; }
    public void setPaymentAlerts(boolean paymentAlerts) { this.paymentAlerts = paymentAlerts; }
    public boolean isLowBalanceAlerts() { return lowBalanceAlerts; }
    public void setLowBalanceAlerts(boolean lowBalanceAlerts) { this.lowBalanceAlerts = lowBalanceAlerts; }
    public boolean isPaperlessStatements() { return paperlessStatements; }
    public void setPaperlessStatements(boolean paperlessStatements) { this.paperlessStatements = paperlessStatements; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
