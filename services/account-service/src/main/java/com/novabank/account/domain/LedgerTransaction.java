package com.novabank.account.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions", indexes = {
        @Index(name = "idx_ledger_transactions_business_operation", columnList = "business_operation_id", unique = true),
        @Index(name = "idx_ledger_transactions_reference", columnList = "reference")
})
public class LedgerTransaction {
    @Id
    private UUID id;

    @Column(name = "business_operation_id", nullable = false, unique = true)
    private UUID businessOperationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LedgerTransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerTransactionStatus status;

    @Column(nullable = false, length = 120)
    private String reference;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getBusinessOperationId() { return businessOperationId; }
    public void setBusinessOperationId(UUID businessOperationId) { this.businessOperationId = businessOperationId; }
    public LedgerTransactionType getType() { return type; }
    public void setType(LedgerTransactionType type) { this.type = type; }
    public LedgerTransactionStatus getStatus() { return status; }
    public void setStatus(LedgerTransactionStatus status) { this.status = status; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
}
