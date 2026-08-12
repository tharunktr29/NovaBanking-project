package com.novabank.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.transaction.domain.*;
import com.novabank.transaction.dto.IngestTransactionRequest;
import com.novabank.transaction.exception.TransactionException;
import com.novabank.transaction.repository.BankTransactionRepository;
import com.novabank.transaction.repository.CustomerAccountRepository;
import com.novabank.transaction.repository.MerchantRepository;
import com.novabank.transaction.repository.OutboxEventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TransactionIngestionService {
    private final BankTransactionRepository transactionRepository;
    private final CustomerAccountRepository accountRepository;
    private final MerchantRepository merchantRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final TransactionMapper mapper;

    public TransactionIngestionService(
            BankTransactionRepository transactionRepository,
            CustomerAccountRepository accountRepository,
            MerchantRepository merchantRepository,
            OutboxEventRepository outboxRepository,
            ObjectMapper objectMapper,
            TransactionMapper mapper
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.merchantRepository = merchantRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
        this.mapper = mapper;
    }

    @Transactional
    public com.novabank.transaction.dto.TransactionResponse ingest(IngestTransactionRequest request, String correlationId) {
        return transactionRepository.findBySourceReference(request.sourceReference())
                .map(mapper::toResponse)
                .orElseGet(() -> mapper.toResponse(create(request, correlationId)));
    }

    @Transactional
    public com.novabank.transaction.dto.TransactionResponse post(UUID customerId, UUID transactionId, String correlationId) {
        var transaction = transactionRepository.findByIdAndCustomerId(transactionId, customerId)
                .orElseThrow(() -> new TransactionException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction was not found"));
        if (transaction.getStatus() == TransactionStatus.POSTED) {
            return mapper.toResponse(transaction);
        }
        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new TransactionException(HttpStatus.CONFLICT, "INVALID_TRANSACTION_TRANSITION", "Only pending transactions can be posted");
        }
        var previousStatus = transaction.getStatus();
        transaction.setStatus(TransactionStatus.POSTED);
        transaction.setPostedAt(Instant.now());
        var saved = transactionRepository.save(transaction);
        createOutbox(saved, "TransactionPosted", correlationId, previousStatus.name());
        return mapper.toResponse(saved);
    }

    private BankTransaction create(IngestTransactionRequest request, String correlationId) {
        validate(request);
        var account = accountRepository.findByAccountIdAndCustomerId(request.accountId(), request.customerId())
                .orElseThrow(() -> new TransactionException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
        if (!account.getCurrency().equalsIgnoreCase(request.currency())) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Transaction currency must match the account currency");
        }
        if (request.merchantId() != null && !merchantRepository.existsById(request.merchantId())) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "MERCHANT_NOT_FOUND", "Merchant was not found");
        }

        var transaction = new BankTransaction();
        transaction.setSourceReference(request.sourceReference().trim());
        transaction.setCustomerId(request.customerId());
        transaction.setAccountId(request.accountId());
        transaction.setMerchantId(request.merchantId());
        transaction.setStatus(request.status());
        transaction.setDirection(request.direction());
        transaction.setType(request.type());
        transaction.setCategory(request.category());
        transaction.setDescription(request.description().trim());
        transaction.setAmount(request.amount().abs());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setAuthorizedAt(request.authorizedAt());
        transaction.setPostedAt(request.postedAt());
        var saved = transactionRepository.save(transaction);
        createOutbox(saved, "TransactionCreated", correlationId, null);
        return saved;
    }

    private void validate(IngestTransactionRequest request) {
        if (request.amount().signum() <= 0) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Transaction amount must be positive");
        }
        if (request.status() == TransactionStatus.PENDING && request.postedAt() != null) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_STATUS", "Pending transactions cannot have postedAt");
        }
        if (request.status() == TransactionStatus.POSTED && request.postedAt() == null) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_STATUS", "Posted transactions must have postedAt");
        }
        if (request.status() == TransactionStatus.REVERSED) {
            throw new TransactionException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_STATUS", "Reversed transactions are not accepted by ingestion");
        }
    }

    private void createOutbox(BankTransaction transaction, String eventType, String correlationId, String previousStatus) {
        var event = new OutboxEvent();
        event.setAggregateId(transaction.getId());
        event.setEventType(eventType);
        event.setCorrelationId(correlationId);
        event.setStatus(OutboxStatus.PENDING);
        event.setAttempts(0);
        event.setPayload(payload(transaction, previousStatus));
        outboxRepository.save(event);
    }

    private String payload(BankTransaction transaction, String previousStatus) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("transactionId", transaction.getId().toString());
        payload.put("sourceReference", transaction.getSourceReference());
        payload.put("customerId", transaction.getCustomerId().toString());
        payload.put("accountId", transaction.getAccountId().toString());
        payload.put("status", transaction.getStatus().name());
        if (previousStatus != null) {
            payload.put("previousStatus", previousStatus);
        }
        payload.put("direction", transaction.getDirection().name());
        payload.put("type", transaction.getType().name());
        payload.put("category", transaction.getCategory().name());
        payload.put("amount", transaction.getAmount().toPlainString());
        payload.put("currency", transaction.getCurrency());
        payload.put("authorizedAt", transaction.getAuthorizedAt().toString());
        if (transaction.getPostedAt() != null) {
            payload.put("postedAt", transaction.getPostedAt().toString());
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new TransactionException(HttpStatus.INTERNAL_SERVER_ERROR, "OUTBOX_SERIALIZATION_FAILED", "Unable to create transaction event");
        }
    }
}
