package com.novabank.card.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_lifecycle_history", indexes = {
        @Index(name = "idx_card_lifecycle_history_card_id", columnList = "card_id"),
        @Index(name = "idx_card_lifecycle_history_occurred_at", columnList = "occurred_at")
})
public class CardLifecycleHistory {
    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CardAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 40)
    private CardStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 40)
    private CardStatus newStatus;

    @Column(name = "reason_code", length = 40)
    private String reasonCode;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public CardAction getAction() { return action; }
    public void setAction(CardAction action) { this.action = action; }
    public CardStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(CardStatus previousStatus) { this.previousStatus = previousStatus; }
    public CardStatus getNewStatus() { return newStatus; }
    public void setNewStatus(CardStatus newStatus) { this.newStatus = newStatus; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
