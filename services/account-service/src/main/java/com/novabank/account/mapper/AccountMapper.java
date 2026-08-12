package com.novabank.account.mapper;

import com.novabank.account.domain.Account;
import com.novabank.account.domain.AccountBalance;
import com.novabank.account.dto.response.AccountBalanceResponse;
import com.novabank.account.dto.response.AccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
    public AccountResponse toResponse(Account account, AccountBalance balance) {
        return new AccountResponse(
                account.getId(),
                account.getCustomerId(),
                account.getAccountType(),
                account.getMaskedAccountNumber(),
                account.getNickname(),
                account.getStatus(),
                account.getCurrency(),
                account.getOpenedDate(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                account.getVersion(),
                balance == null ? null : toBalanceResponse(balance)
        );
    }

    public AccountBalanceResponse toBalanceResponse(AccountBalance balance) {
        return new AccountBalanceResponse(
                balance.getId(),
                balance.getAccountId(),
                balance.getCurrentBalance(),
                balance.getAvailableBalance(),
                balance.getPendingDebitAmount(),
                balance.getPendingCreditAmount(),
                balance.getAsOf(),
                balance.getVersion()
        );
    }
}
