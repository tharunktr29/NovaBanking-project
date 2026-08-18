package com.novabank.card.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "replacement_requests", indexes = {
        @Index(name = "idx_replacement_requests_card_id", columnList = "card_id"),
        @Index(name = "idx_replacement_requests_status", columnList = "status")
})
public class ReplacementRequest {
    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReplacementReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReplacementRequestStatus status;

    @Column(name = "request_reference", nullable = false, unique = true, length = 40)
    private String requestReference;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (requestedAt == null) {
            requestedAt = Instant.now();
        }
        if (status == null) {
            status = ReplacementRequestStatus.REQUESTED;
        }
        if (version == null) {
            version = 0L;
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public ReplacementReason getReason() { return reason; }
    public void setReason(ReplacementReason reason) { this.reason = reason; }
    public ReplacementRequestStatus getStatus() { return status; }
    public void setStatus(ReplacementRequestStatus status) { this.status = status; }
    public String getRequestReference() { return requestReference; }
    public void setRequestReference(String requestReference) { this.requestReference = requestReference; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getVersion() { return version; }
}
