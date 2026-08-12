package com.novabank.transaction.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bank_transactions", indexes = {
        @Index(name = "idx_bank_transactions_customer_id", columnList = "customer_id"),
        @Index(name = "idx_bank_transactions_account_id", columnList = "account_id"),
        @Index(name = "idx_bank_transactions_status", columnList = "status"),
        @Index(name = "idx_bank_transactions_authorized_at", columnList = "authorized_at"),
        @Index(name = "idx_bank_transactions_posted_at", columnList = "posted_at"),
        @Index(name = "idx_bank_transactions_category", columnList = "category"),
        @Index(name = "idx_bank_transactions_merchant_id", columnList = "merchant_id"),
        @Index(name = "idx_bank_transactions_customer_account_authorized", columnList = "customer_id, account_id, authorized_at"),
        @Index(name = "idx_bank_transactions_customer_authorized", columnList = "customer_id, authorized_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_bank_transactions_source_reference", columnNames = "source_reference"))
public class BankTransaction {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "source_reference", nullable = false, unique = true, length = 120)
    private String sourceReference;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Merchant merchant;

    @Column(name = "merchant_id")
    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionDirection direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionCategory category;

    @Column(nullable = false, length = 240)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "authorized_at", nullable = false)
    private Instant authorizedAt;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @PrePersist
    void prePersist() {
        var now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String sourceReference) { this.sourceReference = sourceReference; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getAccountId() { return accountId; }
    public void setAccountId(UUID accountId) { this.accountId = accountId; }
    public Merchant getMerchant() { return merchant; }
    public UUID getMerchantId() { return merchantId; }
    public void setMerchantId(UUID merchantId) { this.merchantId = merchantId; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public TransactionDirection getDirection() { return direction; }
    public void setDirection(TransactionDirection direction) { this.direction = direction; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(Instant authorizedAt) { this.authorizedAt = authorizedAt; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
