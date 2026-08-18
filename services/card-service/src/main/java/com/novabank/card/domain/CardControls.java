package com.novabank.card.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_controls", indexes = {
        @Index(name = "idx_card_controls_card_id", columnList = "card_id")
})
public class CardControls {
    @Id
    private UUID id;

    @Column(name = "card_id", nullable = false, unique = true)
    private UUID cardId;

    @Column(name = "online_purchases_enabled", nullable = false)
    private boolean onlinePurchasesEnabled;

    @Column(name = "contactless_enabled", nullable = false)
    private boolean contactlessEnabled;

    @Column(name = "international_purchases_enabled", nullable = false)
    private boolean internationalPurchasesEnabled;

    @Column(name = "atm_withdrawals_enabled", nullable = false)
    private boolean atmWithdrawalsEnabled;

    @Column(name = "daily_purchase_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyPurchaseLimit;

    @Column(name = "daily_atm_limit", nullable = false, precision = 19, scale = 2)
    private BigDecimal dailyAtmLimit;

    @Column(nullable = false, length = 3)
    private String currency;

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
    public UUID getCardId() { return cardId; }
    public void setCardId(UUID cardId) { this.cardId = cardId; }
    public boolean isOnlinePurchasesEnabled() { return onlinePurchasesEnabled; }
    public void setOnlinePurchasesEnabled(boolean onlinePurchasesEnabled) { this.onlinePurchasesEnabled = onlinePurchasesEnabled; }
    public boolean isContactlessEnabled() { return contactlessEnabled; }
    public void setContactlessEnabled(boolean contactlessEnabled) { this.contactlessEnabled = contactlessEnabled; }
    public boolean isInternationalPurchasesEnabled() { return internationalPurchasesEnabled; }
    public void setInternationalPurchasesEnabled(boolean internationalPurchasesEnabled) { this.internationalPurchasesEnabled = internationalPurchasesEnabled; }
    public boolean isAtmWithdrawalsEnabled() { return atmWithdrawalsEnabled; }
    public void setAtmWithdrawalsEnabled(boolean atmWithdrawalsEnabled) { this.atmWithdrawalsEnabled = atmWithdrawalsEnabled; }
    public BigDecimal getDailyPurchaseLimit() { return dailyPurchaseLimit; }
    public void setDailyPurchaseLimit(BigDecimal dailyPurchaseLimit) { this.dailyPurchaseLimit = dailyPurchaseLimit; }
    public BigDecimal getDailyAtmLimit() { return dailyAtmLimit; }
    public void setDailyAtmLimit(BigDecimal dailyAtmLimit) { this.dailyAtmLimit = dailyAtmLimit; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
