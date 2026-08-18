package com.novabank.card.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.card.domain.IdempotencyRecord;
import com.novabank.card.dto.response.ReplacementRequestResponse;
import com.novabank.card.exception.CardException;
import com.novabank.card.repository.IdempotencyRecordRepository;
import com.novabank.card.validation.IdempotencyKeyValidator;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotencyServiceTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CARD_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final IdempotencyService service = new IdempotencyService(repository, objectMapper, new IdempotencyKeyValidator());

    @Test
    void firstCommandStoresResponse() {
        when(repository.findByCustomerIdAndOperationAndResourceIdAndIdempotencyKey(CUSTOMER_ID, "REPLACE", CARD_ID, "key-12345")).thenReturn(Optional.empty());
        var calls = new AtomicInteger();

        var response = service.run(CUSTOMER_ID, CARD_ID, "REPLACE", "key-12345", new Request("LOST"), ReplacementRequestResponse.class, () -> {
            calls.incrementAndGet();
            return new ReplacementRequestResponse(CARD_ID, "RPL-12345678", null, Instant.parse("2026-08-11T12:00:00Z"), "Replacement request received");
        });

        assertThat(response.requestReference()).isEqualTo("RPL-12345678");
        assertThat(calls).hasValue(1);
        verify(repository).save(any(IdempotencyRecord.class));
    }

    @Test
    void duplicateCommandReplaysStoredResponse() throws Exception {
        var stored = new IdempotencyRecord();
        stored.setCustomerId(CUSTOMER_ID);
        stored.setResourceId(CARD_ID);
        stored.setOperation("REPLACE");
        stored.setIdempotencyKey("key-12345");
        stored.setRequestHash(hash(new Request("LOST")));
        stored.setResponseStatus(200);
        stored.setResponseBody(objectMapper.writeValueAsString(new ReplacementRequestResponse(CARD_ID, "RPL-REPLAY", null, Instant.parse("2026-08-11T12:00:00Z"), "Replacement request received")));
        when(repository.findByCustomerIdAndOperationAndResourceIdAndIdempotencyKey(CUSTOMER_ID, "REPLACE", CARD_ID, "key-12345")).thenReturn(Optional.of(stored));

        var response = service.run(CUSTOMER_ID, CARD_ID, "REPLACE", "key-12345", new Request("LOST"), ReplacementRequestResponse.class, () -> {
            throw new AssertionError("command must not run twice");
        });

        assertThat(response.requestReference()).isEqualTo("RPL-REPLAY");
        verify(repository, never()).save(any());
    }

    @Test
    void reusedKeyWithDifferentRequestIsRejected() {
        var stored = new IdempotencyRecord();
        stored.setRequestHash("different");
        when(repository.findByCustomerIdAndOperationAndResourceIdAndIdempotencyKey(CUSTOMER_ID, "REPLACE", CARD_ID, "key-12345")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.run(CUSTOMER_ID, CARD_ID, "REPLACE", "key-12345", new Request("STOLEN"), ReplacementRequestResponse.class, () -> null))
                .isInstanceOf(CardException.class)
                .hasMessageContaining("different request");
    }

    record Request(String reason) {
    }

    private String hash(Object request) throws Exception {
        var body = objectMapper.writeValueAsString(request);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    }
}
