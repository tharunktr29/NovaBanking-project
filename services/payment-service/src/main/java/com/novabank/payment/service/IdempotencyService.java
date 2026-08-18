package com.novabank.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.payment.domain.IdempotencyRecord;
import com.novabank.payment.exception.PaymentException;
import com.novabank.payment.repository.IdempotencyRecordRepository;
import com.novabank.payment.validation.IdempotencyKeyValidator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class IdempotencyService {
    private static final Duration TTL = Duration.ofHours(24);

    private final IdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;
    private final IdempotencyKeyValidator validator;

    public IdempotencyService(IdempotencyRecordRepository repository, ObjectMapper objectMapper, IdempotencyKeyValidator validator) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public <T> T run(UUID customerId, String operation, String key, Object request, Class<T> responseType, Supplier<T> command) {
        var cleanKey = validator.validate(key);
        var requestHash = hash(request);
        var existing = repository.findByCustomerIdAndOperationAndIdempotencyKey(customerId, operation, cleanKey);
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new PaymentException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency-Key was reused with a different request");
            }
            try {
                return objectMapper.readValue(existing.get().getResponseBody(), responseType);
            } catch (JsonProcessingException ex) {
                throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_REPLAY_FAILED", "Unable to replay idempotent response");
            }
        }

        var response = command.get();
        var record = new IdempotencyRecord();
        record.setCustomerId(customerId);
        record.setOperation(operation);
        record.setIdempotencyKey(cleanKey);
        record.setRequestHash(requestHash);
        record.setResponseStatus(HttpStatus.OK.value());
        record.setExpiresAt(Instant.now().plus(TTL));
        try {
            record.setResponseBody(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_RECORD_FAILED", "Unable to record idempotent response");
        }
        if (response instanceof com.novabank.payment.dto.response.PaymentResponse payment) {
            record.setResourceId(payment.id());
        } else if (response instanceof com.novabank.payment.dto.response.PayeeResponse payee) {
            record.setResourceId(payee.id());
        }
        repository.save(record);
        return response;
    }

    private String hash(Object request) {
        try {
            var body = request == null ? "{}" : objectMapper.writeValueAsString(request);
            var digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new PaymentException(HttpStatus.INTERNAL_SERVER_ERROR, "REQUEST_HASH_FAILED", "Unable to hash request");
        }
    }
}
