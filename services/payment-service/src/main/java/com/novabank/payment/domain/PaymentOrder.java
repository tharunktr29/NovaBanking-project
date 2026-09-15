package com.novabank.payment.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_orders", indexes = {
        @Index(name = "idx_payment_orders_customer_created", columnList = "customer_id, created_at"),
        @Index(name = "idx_payment_orders_customer_status", columnList = "customer_id, status"),
        @Index(name = "idx_payment_orders_source_account", columnList = "source_account_id"),
        @Index(name = "idx_payment_orders_scheduled", columnList = "status, scheduled_for")
})
public class PaymentOrder {
    @Id
    private UUID id;
    @Column(name = "payment_reference", nullable = false, unique = true, length = 60)
    private String paymentReference;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 40)
    private PaymentType paymentType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;
    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;
    @Column(name = "destination_account_id")
    private UUID destinationAccountId;
    @Column(name = "destination_card_id")
    private UUID destinationCardId;
    @Column(name = "external_payee_id")
    private UUID externalPayeeId;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Column(length = 160)
    private String memo;
    @Enumerated(EnumType.STRING)
    @Column(name = "execution_type", nullable = false, length = 20)
    private ExecutionType executionType;
    @Column(name = "scheduled_for")
    private Instant scheduledFor;
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
    @Column(name = "completed_at")
    private Instant completedAt;
    @Column(name = "failed_at")
    private Instant failedAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_code", length = 40)
    private PaymentFailureCode failureCode;
    @Column(name = "failure_message", length = 200)
    private String failureMessage;
    @Column(name = "risk_assessment_id")
    private UUID riskAssessmentId;
    @Column(name = "customer_safe_reason", length = 240)
    private String customerSafeReason;
    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;
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
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(UUID sourceAccountId) { this.sourceAccountId = sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(UUID destinationAccountId) { this.destinationAccountId = destinationAccountId; }
    public UUID getDestinationCardId() { return destinationCardId; }
    public void setDestinationCardId(UUID destinationCardId) { this.destinationCardId = destinationCardId; }
    public UUID getExternalPayeeId() { return externalPayeeId; }
    public void setExternalPayeeId(UUID externalPayeeId) { this.externalPayeeId = externalPayeeId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public ExecutionType getExecutionType() { return executionType; }
    public void setExecutionType(ExecutionType executionType) { this.executionType = executionType; }
    public Instant getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(Instant scheduledFor) { this.scheduledFor = scheduledFor; }
    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public void setProcessingStartedAt(Instant processingStartedAt) { this.processingStartedAt = processingStartedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getFailedAt() { return failedAt; }
    public void setFailedAt(Instant failedAt) { this.failedAt = failedAt; }
    public PaymentFailureCode getFailureCode() { return failureCode; }
    public void setFailureCode(PaymentFailureCode failureCode) { this.failureCode = failureCode; }
    public String getFailureMessage() { return failureMessage; }
    public void setFailureMessage(String failureMessage) { this.failureMessage = failureMessage; }
    public UUID getRiskAssessmentId() { return riskAssessmentId; }
    public void setRiskAssessmentId(UUID riskAssessmentId) { this.riskAssessmentId = riskAssessmentId; }
    public String getCustomerSafeReason() { return customerSafeReason; }
    public void setCustomerSafeReason(String customerSafeReason) { this.customerSafeReason = customerSafeReason; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
