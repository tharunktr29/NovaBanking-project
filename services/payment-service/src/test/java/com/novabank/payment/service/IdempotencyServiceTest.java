package com.novabank.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.payment.domain.IdempotencyRecord;
import com.novabank.payment.dto.response.PayeeResponse;
import com.novabank.payment.exception.PaymentException;
import com.novabank.payment.repository.IdempotencyRecordRepository;
import com.novabank.payment.validation.IdempotencyKeyValidator;
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
    private final IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final IdempotencyService service = new IdempotencyService(repository, objectMapper, new IdempotencyKeyValidator());

    @Test
    void storesFirstResponse() {
        when(repository.findByCustomerIdAndOperationAndIdempotencyKey(CUSTOMER_ID, "CREATE_PAYEE", "key-12345")).thenReturn(Optional.empty());
        var calls = new AtomicInteger();

        var response = service.run(CUSTOMER_ID, "CREATE_PAYEE", "key-12345", new Request("Rent"), PayeeResponse.class, () -> {
            calls.incrementAndGet();
            return payee("PAYEE-1");
        });

        assertThat(response.payeeReference()).isEqualTo("PAYEE-1");
        assertThat(calls).hasValue(1);
        verify(repository).save(any(IdempotencyRecord.class));
    }

    @Test
    void duplicateRequestReplaysStoredResponse() throws Exception {
        var stored = new IdempotencyRecord();
        stored.setCustomerId(CUSTOMER_ID);
        stored.setOperation("CREATE_PAYEE");
        stored.setIdempotencyKey("key-12345");
        stored.setRequestHash(hash(new Request("Rent")));
        stored.setResponseStatus(200);
        stored.setResponseBody(objectMapper.writeValueAsString(payee("PAYEE-REPLAY")));
        when(repository.findByCustomerIdAndOperationAndIdempotencyKey(CUSTOMER_ID, "CREATE_PAYEE", "key-12345")).thenReturn(Optional.of(stored));

        var response = service.run(CUSTOMER_ID, "CREATE_PAYEE", "key-12345", new Request("Rent"), PayeeResponse.class, () -> {
            throw new AssertionError("command should not be re-executed");
        });

        assertThat(response.payeeReference()).isEqualTo("PAYEE-REPLAY");
        verify(repository, never()).save(any());
    }

    @Test
    void reusedKeyWithDifferentRequestIsRejected() {
        var stored = new IdempotencyRecord();
        stored.setRequestHash("different");
        when(repository.findByCustomerIdAndOperationAndIdempotencyKey(CUSTOMER_ID, "CREATE_PAYEE", "key-12345")).thenReturn(Optional.of(stored));

        assertThatThrownBy(() -> service.run(CUSTOMER_ID, "CREATE_PAYEE", "key-12345", new Request("Utilities"), PayeeResponse.class, () -> payee("PAYEE-1")))
                .isInstanceOf(PaymentException.class)
                .hasMessageContaining("different request");
    }

    record Request(String nickname) {
    }

    private PayeeResponse payee(String reference) {
        var now = Instant.parse("2026-08-13T00:00:00Z");
        return new PayeeResponse(UUID.randomUUID(), reference, "Rent", "Demo Bank", com.novabank.payment.domain.ExternalAccountType.CHECKING, "**** 1234", com.novabank.payment.domain.PayeeStatus.VERIFIED, now, now);
    }

    private String hash(Object request) throws Exception {
        var body = objectMapper.writeValueAsString(request);
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
    }
}
