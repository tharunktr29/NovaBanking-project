package com.novabank.account.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_balances", indexes = @Index(name = "idx_account_balances_account_id", columnList = "account_id", unique = true))
public class AccountBalance {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "current_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "available_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "pending_debit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingDebitAmount;

    @Column(name = "pending_credit_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal pendingCreditAmount;

    @Column(name = "as_of", nullable = false)
    private Instant asOf;

    @Version
    private Long version;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public BigDecimal getCurrentBalance() { return currentBalance; }
    public void setCurrentBalance(BigDecimal currentBalance) { this.currentBalance = currentBalance; }
    public BigDecimal getAvailableBalance() { return availableBalance; }
    public void setAvailableBalance(BigDecimal availableBalance) { this.availableBalance = availableBalance; }
    public BigDecimal getPendingDebitAmount() { return pendingDebitAmount; }
    public void setPendingDebitAmount(BigDecimal pendingDebitAmount) { this.pendingDebitAmount = pendingDebitAmount; }
    public BigDecimal getPendingCreditAmount() { return pendingCreditAmount; }
    public void setPendingCreditAmount(BigDecimal pendingCreditAmount) { this.pendingCreditAmount = pendingCreditAmount; }
    public Instant getAsOf() { return asOf; }
    public void setAsOf(Instant asOf) { this.asOf = asOf; }
    public Long getVersion() { return version; }
}
