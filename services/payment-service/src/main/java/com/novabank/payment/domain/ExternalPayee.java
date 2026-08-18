package com.novabank.payment.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_payees", indexes = {
        @Index(name = "idx_external_payees_customer", columnList = "customer_id"),
        @Index(name = "idx_external_payees_status", columnList = "status")
})
public class ExternalPayee {
    @Id
    private UUID id;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "payee_reference", nullable = false, unique = true, length = 60)
    private String payeeReference;
    @Column(nullable = false, length = 80)
    private String nickname;
    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private ExternalAccountType accountType;
    @Column(name = "masked_account_number", nullable = false, length = 32)
    private String maskedAccountNumber;
    @Column(name = "external_account_token", nullable = false, length = 120)
    private String externalAccountToken;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PayeeStatus status;
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
        if (id == null) id = UUID.randomUUID();
        createdAt = now;
        updatedAt = now;
        if (version == null) version = 0L;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getPayeeReference() { return payeeReference; }
    public void setPayeeReference(String payeeReference) { this.payeeReference = payeeReference; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public ExternalAccountType getAccountType() { return accountType; }
    public void setAccountType(ExternalAccountType accountType) { this.accountType = accountType; }
    public String getMaskedAccountNumber() { return maskedAccountNumber; }
    public void setMaskedAccountNumber(String maskedAccountNumber) { this.maskedAccountNumber = maskedAccountNumber; }
    public String getExternalAccountToken() { return externalAccountToken; }
    public void setExternalAccountToken(String externalAccountToken) { this.externalAccountToken = externalAccountToken; }
    public PayeeStatus getStatus() { return status; }
    public void setStatus(PayeeStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
