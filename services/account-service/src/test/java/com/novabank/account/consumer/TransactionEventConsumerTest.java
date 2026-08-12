package com.novabank.account.consumer;

import com.novabank.account.domain.*;
import com.novabank.account.event.AccountEventPublisher;
import com.novabank.account.repository.AccountBalanceRepository;
import com.novabank.account.repository.AccountRepository;
import com.novabank.account.repository.ProcessedEventRepository;
import com.novabank.shared.events.BankingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionEventConsumerTest {
    @Mock AccountRepository accountRepository;
    @Mock AccountBalanceRepository balanceRepository;
    @Mock ProcessedEventRepository processedEventRepository;
    @Mock AccountEventPublisher eventPublisher;

    @Test
    void pendingDebitReducesAvailableAndIncreasesPendingDebitOnce() {
        var consumer = consumer();
        var account = account();
        var balance = balance();
        var event = event("TransactionCreated", Map.of(
                "accountId", account.getId().toString(),
                "status", "PENDING",
                "direction", "DEBIT",
                "amount", "25.00"
        ));
        when(processedEventRepository.existsById(event.eventId())).thenReturn(false);
        when(accountRepository.findByIdAndCustomerId(account.getId(), event.customerId())).thenReturn(Optional.of(account));
        when(balanceRepository.findByAccountId(account.getId())).thenReturn(Optional.of(balance));

        consumer.onTransactionEvent(event);

        assertThat(balance.getPendingDebitAmount()).isEqualByComparingTo("25.00");
        assertThat(balance.getAvailableBalance()).isEqualByComparingTo("975.00");
        assertThat(balance.getCurrentBalance()).isEqualByComparingTo("1000.00");
        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
        verify(eventPublisher).publish(eq("balance.updated"), any(BankingEvent.class));
    }

    @Test
    void duplicateEventIsIgnoredBeforeBalanceMutation() {
        var consumer = consumer();
        var event = event("TransactionCreated", Map.of(
                "accountId", UUID.randomUUID().toString(),
                "status", "PENDING",
                "direction", "DEBIT",
                "amount", "25.00"
        ));
        when(processedEventRepository.existsById(event.eventId())).thenReturn(true);

        consumer.onTransactionEvent(event);

        verifyNoInteractions(accountRepository, balanceRepository, eventPublisher);
    }

    private TransactionEventConsumer consumer() {
        return new TransactionEventConsumer(accountRepository, balanceRepository, processedEventRepository, eventPublisher);
    }

    private Account account() {
        var account = new Account();
        account.setId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        account.setCustomerId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        account.setAccountType(AccountType.CHECKING);
        account.setStatus(AccountStatus.ACTIVE);
        account.setCurrency("USD");
        account.setMaskedAccountNumber("**** 4821");
        account.setNickname("Daily Checking");
        account.setOpenedDate(LocalDate.parse("2024-01-16"));
        return account;
    }

    private AccountBalance balance() {
        var balance = new AccountBalance();
        balance.setAccountId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
        balance.setCurrentBalance(new BigDecimal("1000.00"));
        balance.setAvailableBalance(new BigDecimal("1000.00"));
        balance.setPendingDebitAmount(new BigDecimal("0.00"));
        balance.setPendingCreditAmount(new BigDecimal("0.00"));
        balance.setAsOf(Instant.parse("2026-08-11T12:00:00Z"));
        return balance;
    }

    private BankingEvent event(String type, Map<String, Object> payload) {
        return new BankingEvent(
                UUID.randomUUID(),
                type,
                1,
                Instant.now(),
                "corr",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.randomUUID(),
                payload
        );
    }
}
