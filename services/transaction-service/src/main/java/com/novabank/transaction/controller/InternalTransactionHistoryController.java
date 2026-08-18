package com.novabank.transaction.controller;

import com.novabank.shared.correlation.Correlation;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.dto.HistoryTransactionRequest;
import com.novabank.transaction.dto.IngestTransactionRequest;
import com.novabank.transaction.dto.TransactionResponse;
import com.novabank.transaction.service.TransactionIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/transactions")
public class InternalTransactionHistoryController {
    private final TransactionIngestionService ingestionService;

    public InternalTransactionHistoryController(TransactionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/history")
    TransactionResponse history(Authentication authentication, @Valid @RequestBody HistoryTransactionRequest request, HttpServletRequest servletRequest) {
        return ingestionService.ingest(new IngestTransactionRequest(
                request.sourceReference(),
                UUID.fromString(authentication.getName()),
                request.accountId(),
                null,
                TransactionStatus.POSTED,
                request.direction(),
                request.type(),
                request.category(),
                request.description(),
                request.amount(),
                request.currency(),
                request.postedAt(),
                request.postedAt()
        ), servletRequest.getHeader(Correlation.HEADER_NAME));
    }
}
