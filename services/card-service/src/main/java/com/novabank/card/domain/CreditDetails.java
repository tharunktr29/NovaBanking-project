package com.novabank.card.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "credit_details", indexes = {
        @Index(name = "idx_credit_details_card_id", columnList = "card_id")
})
public class CreditDetails {
    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false, unique = true)
    private UUID cardId;

    @Column(name = "credit_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "available_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCredit;

    @Column(name = "minimum_payment_due", nullable = false, precision = 19, scale = 2)
    private BigDecimal minimumPaymentDue;

    @Column(name = "payment_due_date", nullable = false)
    private LocalDate paymentDueDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        validateInvariant();
        updatedAt = Instant.now();
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        validateInvariant();
        updatedAt = Instant.now();
    }

    private void validateInvariant() {
        if (creditLimit != null && currentBalance != null && availableCredit != null
                && creditLimit.subtract(currentBalance).compareTo(availableCredit) != 0) {
            throw new IllegalStateException("availableCredit must equal creditLimit minus currentBalance");
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getAvailableCredit() { return availableCredit; }
    public void setAvailableCredit(BigDecimal availableCredit) { this.availableCredit = availableCredit; }
    public BigDecimal getMinimumPaymentDue() { return minimumPaymentDue; }
    public void setMinimumPaymentDue(BigDecimal minimumPaymentDue) { this.minimumPaymentDue = minimumPaymentDue; }
    public LocalDate getPaymentDueDate() { return paymentDueDate; }
    public void setPaymentDueDate(LocalDate paymentDueDate) { this.paymentDueDate = paymentDueDate; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
