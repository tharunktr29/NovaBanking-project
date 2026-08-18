package com.novabank.card.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.card.domain.IdempotencyRecord;
import com.novabank.card.exception.CardException;
import com.novabank.card.repository.IdempotencyRecordRepository;
import com.novabank.card.validation.IdempotencyKeyValidator;
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

    public <T> T run(
            UUID customerId,
            UUID resourceId,
            String operation,
            String key,
            Object request,
            Class<T> responseType,
            Supplier<T> command
    ) {
        var cleanKey = validator.validate(key);
        var requestHash = hash(request);
        var existing = repository.findByCustomerIdAndOperationAndResourceIdAndIdempotencyKey(
                customerId,
                operation,
                resourceId,
                cleanKey
        );
        if (existing.isPresent()) {
            if (!existing.get().getRequestHash().equals(requestHash)) {
                throw new CardException(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT", "Idempotency-Key was reused with a different request");
            }
            try {
                return objectMapper.readValue(existing.get().getResponseBody(), responseType);
            } catch (JsonProcessingException ex) {
                throw new CardException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_REPLAY_FAILED", "Unable to replay idempotent response");
            }
        }

        var response = command.get();
        var record = new IdempotencyRecord();
        record.setCustomerId(customerId);
        record.setResourceId(resourceId);
        record.setOperation(operation);
        record.setIdempotencyKey(cleanKey);
        record.setRequestHash(requestHash);
        record.setResponseStatus(HttpStatus.OK.value());
        record.setExpiresAt(Instant.now().plus(TTL));
        try {
            record.setResponseBody(objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException ex) {
            throw new CardException(HttpStatus.INTERNAL_SERVER_ERROR, "IDEMPOTENCY_RECORD_FAILED", "Unable to record idempotent response");
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
            throw new CardException(HttpStatus.INTERNAL_SERVER_ERROR, "REQUEST_HASH_FAILED", "Unable to hash request");
        }
    }
}
