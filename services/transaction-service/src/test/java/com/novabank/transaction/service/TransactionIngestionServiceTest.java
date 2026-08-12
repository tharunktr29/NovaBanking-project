package com.novabank.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novabank.transaction.domain.*;
import com.novabank.transaction.dto.IngestTransactionRequest;
import com.novabank.transaction.exception.TransactionException;
import com.novabank.transaction.repository.BankTransactionRepository;
import com.novabank.transaction.repository.CustomerAccountRepository;
import com.novabank.transaction.repository.MerchantRepository;
import com.novabank.transaction.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionIngestionServiceTest {
    @Mock BankTransactionRepository transactionRepository;
    @Mock CustomerAccountRepository accountRepository;
    @Mock MerchantRepository merchantRepository;
    @Mock OutboxEventRepository outboxRepository;

    @Test
    void duplicateSourceReferenceReturnsExistingTransactionWithoutOutbox() {
        var existing = transaction("existing-source", TransactionStatus.PENDING);
        when(transactionRepository.findBySourceReference("existing-source")).thenReturn(Optional.of(existing));
        var service = service();

        var response = service.ingest(request("existing-source", TransactionStatus.PENDING), "corr-1");

        assertThat(response.transactionId()).isEqualTo(existing.getId());
        verify(transactionRepository, never()).save(any());
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void createsTransactionAndOutboxRecord() {
        var account = account();
        when(transactionRepository.findBySourceReference("new-source")).thenReturn(Optional.empty());
        when(accountRepository.findByAccountIdAndCustomerId(account.getAccountId(), account.getCustomerId())).thenReturn(Optional.of(account));
        when(transactionRepository.save(any())).thenAnswer(invocation -> {
            var transaction = invocation.getArgument(0, BankTransaction.class);
            transaction.setId(UUID.fromString("20000000-0000-0000-0000-000000000099"));
            return transaction;
        });
        var service = service();

        var response = service.ingest(request("new-source", TransactionStatus.PENDING), "corr-2");

        assertThat(response.status()).isEqualTo(TransactionStatus.PENDING);
        var outbox = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(outbox.capture());
        assertThat(outbox.getValue().getEventType()).isEqualTo("TransactionCreated");
        assertThat(outbox.getValue().getPayload()).contains("new-source");
    }

    @Test
    void rejectsInvalidPendingToPostedTransition() {
        var reversed = transaction("source", TransactionStatus.REVERSED);
        when(transactionRepository.findByIdAndCustomerId(reversed.getId(), reversed.getCustomerId())).thenReturn(Optional.of(reversed));
        var service = service();

        assertThatThrownBy(() -> service.post(reversed.getCustomerId(), reversed.getId(), "corr"))
                .isInstanceOf(TransactionException.class)
                .hasMessageContaining("Only pending transactions can be posted");
    }

    private TransactionIngestionService service() {
        return new TransactionIngestionService(
                transactionRepository,
                accountRepository,
                merchantRepository,
                outboxRepository,
                new ObjectMapper().findAndRegisterModules(),
                new TransactionMapper()
        );
    }

    private IngestTransactionRequest request(String source, TransactionStatus status) {
        return new IngestTransactionRequest(
                source,
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                null,
                status,
                TransactionDirection.DEBIT,
                TransactionType.PURCHASE,
                TransactionCategory.GROCERIES,
                "Groceries",
                new BigDecimal("12.34"),
                "USD",
                Instant.parse("2026-08-11T12:00:00Z"),
                status == TransactionStatus.POSTED ? Instant.parse("2026-08-11T13:00:00Z") : null
        );
    }

    private CustomerAccount account() {
        var account = new CustomerAccount();
        account.setAccountId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        account.setCustomerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setCurrency("USD");
        return account;
    }

    private BankTransaction transaction(String sourceReference, TransactionStatus status) {
        var transaction = new BankTransaction();
        transaction.setId(UUID.fromString("20000000-0000-0000-0000-000000000001"));
        transaction.setSourceReference(sourceReference);
        transaction.setCustomerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        transaction.setAccountId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        transaction.setStatus(status);
        transaction.setDirection(TransactionDirection.DEBIT);
        transaction.setType(TransactionType.PURCHASE);
        transaction.setCategory(TransactionCategory.GROCERIES);
        transaction.setDescription("Groceries");
        transaction.setAmount(new BigDecimal("12.34"));
        transaction.setCurrency("USD");
        transaction.setAuthorizedAt(Instant.parse("2026-08-11T12:00:00Z"));
        transaction.setPostedAt(status == TransactionStatus.PENDING ? null : Instant.parse("2026-08-11T13:00:00Z"));
        return transaction;
    }
}
