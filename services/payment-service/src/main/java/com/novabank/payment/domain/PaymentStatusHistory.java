package com.novabank.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_status_history", indexes = {
        @Index(name = "idx_payment_status_history_payment", columnList = "payment_id"),
        @Index(name = "idx_payment_status_history_occurred", columnList = "occurred_at")
})
public class PaymentStatusHistory {
    @Id
    private UUID id;
    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private PaymentStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private PaymentStatus newStatus;
    @Column(name = "reason_code", length = 60)
    private String reasonCode;
    @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
        if (occurredAt == null) occurredAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public UUID getPaymentId() { return paymentId; }
    public PaymentStatus getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(PaymentStatus previousStatus) { this.previousStatus = previousStatus; }
    public PaymentStatus getNewStatus() { return newStatus; }
    public void setNewStatus(PaymentStatus newStatus) { this.newStatus = newStatus; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
}
