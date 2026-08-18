package com.novabank.card.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_payment_applications", indexes = {
        @Index(name = "idx_card_payment_applications_business_operation", columnList = "business_operation_id", unique = true),
        @Index(name = "idx_card_payment_applications_card_id", columnList = "card_id")
})
public class CardPaymentApplication {
    @Id
    private UUID id;

    @Column(name = "business_operation_id", nullable = false, unique = true)
    private UUID businessOperationId;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getBusinessOperationId() { return businessOperationId; }
    public void setBusinessOperationId(UUID businessOperationId) { this.businessOperationId = businessOperationId; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public Instant getAppliedAt() { return appliedAt; }
}
