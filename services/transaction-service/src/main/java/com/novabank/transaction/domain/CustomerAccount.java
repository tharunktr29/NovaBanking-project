package com.novabank.transaction.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_accounts", indexes = {
        @Index(name = "idx_customer_accounts_customer_id", columnList = "customer_id"),
        @Index(name = "idx_customer_accounts_customer_account", columnList = "customer_id, account_id")
})
public class CustomerAccount {
    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(nullable = false, length = 80)
    private String nickname;

    @Column(name = "masked_account_number", nullable = false, length = 32)
    private String maskedAccountNumber;

    @Column(name = "account_type", nullable = false, length = 20)
    private String accountType;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getAccountType() { return accountType; }
    public void setAccountType(String accountType) { this.accountType = accountType; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
