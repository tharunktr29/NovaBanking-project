package com.novabank.auth.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = @Index(name = "idx_audit_logs_user_time", columnList = "user_id, created_at"))
public class AuditLog {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "correlation_id", length = 80)
    private String correlationId;

    @Column(length = 500)
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(UUID userId, String action, String correlationId, String metadata) {
        this.userId = userId;
        this.action = action;
        this.correlationId = correlationId;
        this.metadata = metadata;
    }
}
