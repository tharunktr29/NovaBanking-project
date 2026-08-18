package com.novabank.account.controller;

import com.novabank.account.dto.request.PostLedgerTransactionRequest;
import com.novabank.account.dto.response.LedgerTransactionResponse;
import com.novabank.account.service.LedgerService;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/ledger")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/transactions")
    LedgerTransactionResponse post(Authentication authentication, @Valid @RequestBody PostLedgerTransactionRequest request, HttpServletRequest servletRequest) {
        return ledgerService.post(UUID.fromString(authentication.getName()), request, servletRequest.getHeader(Correlation.HEADER_NAME));
    }
}
