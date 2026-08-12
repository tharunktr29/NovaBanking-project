package com.novabank.account.service;

import com.novabank.account.domain.Account;
import com.novabank.account.domain.AccountStatus;
import com.novabank.account.dto.request.UpdateAccountNicknameRequest;
import com.novabank.account.dto.response.AccountBalanceResponse;
import com.novabank.account.dto.response.AccountResponse;
import com.novabank.account.event.AccountEventPublisher;
import com.novabank.account.exception.AccountException;
import com.novabank.account.mapper.AccountMapper;
import com.novabank.account.repository.AccountBalanceRepository;
import com.novabank.account.repository.AccountRepository;
import com.novabank.shared.events.BankingEvent;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;
    private final AccountMapper mapper;
    private final AccountEventPublisher eventPublisher;

    public AccountService(
            AccountRepository accountRepository,
            AccountBalanceRepository balanceRepository,
            AccountMapper mapper,
            AccountEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(UUID customerId) {
        return accountRepository.findByCustomerIdOrderByAccountTypeAscOpenedDateAsc(customerId).stream()
                .map(account -> mapper.toResponse(account, balanceRepository.findByAccountId(account.getId()).orElse(null)))
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID customerId, UUID accountId) {
        var account = ownedAccount(customerId, accountId);
        return mapper.toResponse(account, balanceRepository.findByAccountId(account.getId()).orElse(null));
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance(UUID customerId, UUID accountId) {
        var account = ownedAccount(customerId, accountId);
        var balance = balanceRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_BALANCE_NOT_FOUND", "Account balance was not found"));
        return mapper.toBalanceResponse(balance);
    }

    @Transactional
    public AccountResponse updateNickname(UUID customerId, UUID accountId, UpdateAccountNicknameRequest request, String correlationId) {
        var account = ownedAccount(customerId, accountId);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountException(HttpStatus.CONFLICT, "ACCOUNT_CLOSED", "Closed accounts cannot be renamed");
        }
        account.setNickname(request.nickname().trim());
        var saved = accountRepository.save(account);
        eventPublisher.publish("account.nickname-updated", BankingEvent.v1(
                "AccountNicknameUpdated",
                correlationId,
                customerId,
                saved.getId(),
                Map.of("accountType", saved.getAccountType().name())
        ));
        return mapper.toResponse(saved, balanceRepository.findByAccountId(saved.getId()).orElse(null));
    }

    private Account ownedAccount(UUID customerId, UUID accountId) {
        return accountRepository.findByIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
    }
}
