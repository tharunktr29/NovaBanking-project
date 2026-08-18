package com.novabank.card.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_cards", indexes = {
        @Index(name = "idx_bank_cards_customer_id", columnList = "customer_id"),
        @Index(name = "idx_bank_cards_account_id", columnList = "account_id"),
        @Index(name = "idx_bank_cards_customer_account", columnList = "customer_id, account_id"),
        @Index(name = "idx_bank_cards_status", columnList = "status"),
        @Index(name = "idx_bank_cards_card_type", columnList = "card_type")
})
public class BankCard {
    @Id
    private UUID id;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "card_reference", nullable = false, unique = true, length = 40)
    private String cardReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false, length = 20)
    private CardType cardType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_network", nullable = false, length = 20)
    private CardNetwork cardNetwork;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CardStatus status;

    @Column(name = "cardholder_name", nullable = false, length = 120)
    private String cardholderName;

    @Column(name = "last_four", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "masked_card_number", nullable = false, length = 32)
    private String maskedCardNumber;

    @Column(name = "expiration_month", nullable = false)
    private Integer expirationMonth;

    @Column(name = "expiration_year", nullable = false)
    private Integer expirationYear;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = now;
        updatedAt = now;
        if (version == null) {
            version = 0L;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public String getCardReference() { return cardReference; }
    public void setCardReference(String cardReference) { this.cardReference = cardReference; }
    public CardType getCardType() { return cardType; }
    public void setCardType(CardType cardType) { this.cardType = cardType; }
    public CardNetwork getCardNetwork() { return cardNetwork; }
    public void setCardNetwork(CardNetwork cardNetwork) { this.cardNetwork = cardNetwork; }
    public CardStatus getStatus() { return status; }
    public void setStatus(CardStatus status) { this.status = status; }
    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    public String getLastFour() { return lastFour; }
    public void setLastFour(String lastFour) { this.lastFour = lastFour; }
    public String getMaskedCardNumber() { return maskedCardNumber; }
    public void setMaskedCardNumber(String maskedCardNumber) { this.maskedCardNumber = maskedCardNumber; }
    public Integer getExpirationMonth() { return expirationMonth; }
    public void setExpirationMonth(Integer expirationMonth) { this.expirationMonth = expirationMonth; }
    public Integer getExpirationYear() { return expirationYear; }
    public void setExpirationYear(Integer expirationYear) { this.expirationYear = expirationYear; }
    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }
    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
