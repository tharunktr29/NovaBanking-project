package com.novabank.account.controller;

import com.novabank.account.dto.request.UpdateAccountNicknameRequest;
import com.novabank.account.dto.response.AccountBalanceResponse;
import com.novabank.account.dto.response.AccountResponse;
import com.novabank.account.service.AccountService;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    List<AccountResponse> accounts(Authentication authentication) {
        return accountService.getAccounts(currentCustomerId(authentication));
    }

    @GetMapping("/{accountId}")
    AccountResponse account(Authentication authentication, @PathVariable("accountId") UUID accountId) {
        return accountService.getAccount(currentCustomerId(authentication), accountId);
    }

    @GetMapping("/{accountId}/balance")
    AccountBalanceResponse balance(Authentication authentication, @PathVariable("accountId") UUID accountId) {
        return accountService.getBalance(currentCustomerId(authentication), accountId);
    }

    @PatchMapping("/{accountId}/nickname")
    AccountResponse updateNickname(
            Authentication authentication,
            @PathVariable("accountId") UUID accountId,
            @Valid @RequestBody UpdateAccountNicknameRequest request,
            HttpServletRequest servletRequest
    ) {
        return accountService.updateNickname(
                currentCustomerId(authentication),
                accountId,
                request,
                servletRequest.getHeader(Correlation.HEADER_NAME)
        );
    }

    private UUID currentCustomerId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
