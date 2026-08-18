package com.novabank.payment.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_payment_idempotency_customer_operation_key", columnNames = {"customer_id", "operation", "idempotency_key"}),
        indexes = @Index(name = "idx_payment_idempotency_expires", columnList = "expires_at"))
public class IdempotencyRecord {
    @Id
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;
    @Column(nullable = false, length = 80)
    private String operation;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "resource_id")
    private UUID resourceId;
    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;
    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responseBody;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String requestHash) { this.requestHash = requestHash; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public Integer getResponseStatus() { return responseStatus; }
    public void setResponseStatus(Integer responseStatus) { this.responseStatus = responseStatus; }
    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
}
