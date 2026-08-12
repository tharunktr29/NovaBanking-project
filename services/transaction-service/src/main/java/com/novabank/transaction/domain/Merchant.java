package com.novabank.transaction.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "merchants", indexes = {
        @Index(name = "idx_merchants_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_merchants_category", columnList = "category")
})
public class Merchant {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 160)
    private String normalizedName;

    @Column(name = "merchant_category_code", length = 16)
    private String merchantCategoryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionCategory category;

    @Column(length = 80)
    private String city;

    @Column(length = 40)
    private String state;

    @Column(length = 80)
    private String country;

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
        if (normalizedName == null && name != null) {
            normalizedName = name.trim().toLowerCase();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
        if (name != null) {
            normalizedName = name.trim().toLowerCase();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNormalizedName() { return normalizedName; }
    public void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
    public String getMerchantCategoryCode() { return merchantCategoryCode; }
    public void setMerchantCategoryCode(String merchantCategoryCode) { this.merchantCategoryCode = merchantCategoryCode; }
    public TransactionCategory getCategory() { return category; }
    public void setCategory(TransactionCategory category) { this.category = category; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
