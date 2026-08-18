package com.novabank.account.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_entries_transaction", columnList = "ledger_transaction_id"),
        @Index(name = "idx_ledger_entries_account", columnList = "account_id")
})
public class LedgerEntry {
    @Id
    private UUID id;

    @Column(name = "ledger_transaction_id", nullable = false)
    private UUID ledgerTransactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LedgerEntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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
    public UUID getLedgerTransactionId() { return ledgerTransactionId; }
    public void setLedgerTransactionId(UUID ledgerTransactionId) { this.ledgerTransactionId = ledgerTransactionId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public LedgerEntryDirection getDirection() { return direction; }
    public void setDirection(LedgerEntryDirection direction) { this.direction = direction; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public Instant getCreatedAt() { return createdAt; }
}
