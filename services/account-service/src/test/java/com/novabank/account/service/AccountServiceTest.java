package com.novabank.account.service;

import com.novabank.account.domain.Account;
import com.novabank.account.domain.AccountBalance;
import com.novabank.account.domain.AccountStatus;
import com.novabank.account.domain.AccountType;
import com.novabank.account.dto.request.UpdateAccountNicknameRequest;
import com.novabank.account.event.AccountEventPublisher;
import com.novabank.account.exception.AccountException;
import com.novabank.account.mapper.AccountMapper;
import com.novabank.account.repository.AccountBalanceRepository;
import com.novabank.account.repository.AccountRepository;
import com.novabank.shared.events.BankingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ACCOUNT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Mock
    AccountRepository accountRepository;

    @Mock
    AccountBalanceRepository balanceRepository;

    @Mock
    AccountEventPublisher eventPublisher;

    @Test
    void customerRetrievesCheckingAndSavingsAccountsWithBalances() {
        var service = service();
        var checking = account(ACCOUNT_ID, AccountType.CHECKING, AccountStatus.ACTIVE);
        var savings = account(UUID.fromString("55555555-5555-5555-5555-555555555555"), AccountType.SAVINGS, AccountStatus.ACTIVE);
        when(accountRepository.findByCustomerIdOrderByAccountTypeAscOpenedDateAsc(CUSTOMER_ID)).thenReturn(List.of(checking, savings));
        when(balanceRepository.findByAccountId(checking.getId())).thenReturn(Optional.of(balance(checking.getId())));
        when(balanceRepository.findByAccountId(savings.getId())).thenReturn(Optional.of(balance(savings.getId())));

        var response = service.getAccounts(CUSTOMER_ID);

        assertThat(response).hasSize(2);
        assertThat(response.getFirst().maskedAccountNumber()).isEqualTo("**** 4821");
        assertThat(response.getFirst().maskedAccountNumber()).doesNotContain("123456789");
        assertThat(response.getFirst().balance().currentBalance()).isEqualByComparingTo("4286.42");
    }

    @Test
    void customerRetrievesAccountBalanceForOwnedAccount() {
        var service = service();
        var account = account(ACCOUNT_ID, AccountType.CHECKING, AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndCustomerId(ACCOUNT_ID, CUSTOMER_ID)).thenReturn(Optional.of(account));
        when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(balance(ACCOUNT_ID)));

        var response = service.getBalance(CUSTOMER_ID, ACCOUNT_ID);

        assertThat(response.availableBalance()).isEqualByComparingTo("4036.42");
    }

    @Test
    void customerCannotAccessAnotherCustomersAccount() {
        var service = service();
        when(accountRepository.findByIdAndCustomerId(ACCOUNT_ID, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getAccount(CUSTOMER_ID, ACCOUNT_ID))
                .isInstanceOf(AccountException.class)
                .hasMessage("Account was not found");
    }

    @Test
    void customerChangesAccountNickname() {
        var service = service();
        var account = account(ACCOUNT_ID, AccountType.CHECKING, AccountStatus.ACTIVE);
        when(accountRepository.findByIdAndCustomerId(ACCOUNT_ID, CUSTOMER_ID)).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(balanceRepository.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(balance(ACCOUNT_ID)));

        var response = service.updateNickname(CUSTOMER_ID, ACCOUNT_ID, new UpdateAccountNicknameRequest(" Bills Checking "), "corr-3");

        assertThat(response.nickname()).isEqualTo("Bills Checking");
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("account.nickname-updated"), any(BankingEvent.class));
    }

    @Test
    void closedAccountNicknameUpdateIsRejected() {
        var service = service();
        when(accountRepository.findByIdAndCustomerId(ACCOUNT_ID, CUSTOMER_ID))
                .thenReturn(Optional.of(account(ACCOUNT_ID, AccountType.CHECKING, AccountStatus.CLOSED)));

        assertThatThrownBy(() -> service.updateNickname(CUSTOMER_ID, ACCOUNT_ID, new UpdateAccountNicknameRequest("New Name"), "corr-4"))
                .isInstanceOf(AccountException.class)
                .hasMessage("Closed accounts cannot be renamed");
    }

    private AccountService service() {
        return new AccountService(accountRepository, balanceRepository, new AccountMapper(), eventPublisher);
    }

    private Account account(UUID id, AccountType accountType, AccountStatus status) {
        var account = new Account();
        account.setId(id);
        account.setCustomerId(CUSTOMER_ID);
        account.setAccountType(accountType);
        account.setMaskedAccountNumber(accountType == AccountType.CHECKING ? "**** 4821" : "**** 7710");
        account.setNickname(accountType == AccountType.CHECKING ? "Daily Checking" : "Emergency Savings");
        account.setStatus(status);
        account.setCurrency("USD");
        account.setOpenedDate(LocalDate.of(2024, 1, 16));
        return account;
    }

    private AccountBalance balance(UUID accountId) {
        var balance = new AccountBalance();
        balance.setId(UUID.randomUUID());
        balance.setAccountId(accountId);
        balance.setCurrentBalance(new BigDecimal("4286.42"));
        balance.setAvailableBalance(new BigDecimal("4036.42"));
        balance.setPendingDebitAmount(new BigDecimal("250.00"));
        balance.setPendingCreditAmount(new BigDecimal("0.00"));
        balance.setAsOf(Instant.parse("2026-08-11T12:00:00Z"));
        return balance;
    }
}
