package com.novabank.transaction.controller;

import com.novabank.transaction.dto.PageResponse;
import com.novabank.transaction.dto.TransactionResponse;
import com.novabank.transaction.service.TransactionCsvExporter;
import com.novabank.transaction.service.TransactionQueryParser;
import com.novabank.transaction.service.TransactionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final TransactionCsvExporter csvExporter;
    private final TransactionQueryParser queryParser;

    public TransactionController(TransactionService transactionService, TransactionCsvExporter csvExporter, TransactionQueryParser queryParser) {
        this.transactionService = transactionService;
        this.csvExporter = csvExporter;
        this.queryParser = queryParser;
    }

    @GetMapping
    PageResponse<TransactionResponse> transactions(
            Authentication authentication,
            @RequestParam(name = "accountId", required = false) String accountId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "merchant", required = false) String merchant,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo,
            @RequestParam(name = "minAmount", required = false) String minAmount,
            @RequestParam(name = "maxAmount", required = false) String maxAmount,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        return transactionService.list(currentCustomerId(authentication), queryParser.parse(
                accountId, status, direction, type, category, merchant, search,
                dateFrom, dateTo, minAmount, maxAmount, page, size, sort
        ));
    }

    @GetMapping("/{transactionId}")
    TransactionResponse transaction(Authentication authentication, @PathVariable UUID transactionId) {
        return transactionService.detail(currentCustomerId(authentication), transactionId);
    }

    @GetMapping("/export")
    ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> export(
            Authentication authentication,
            @RequestParam(name = "accountId", required = false) String accountId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "direction", required = false) String direction,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "category", required = false) String category,
            @RequestParam(name = "merchant", required = false) String merchant,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "dateFrom", required = false) String dateFrom,
            @RequestParam(name = "dateTo", required = false) String dateTo,
            @RequestParam(name = "minAmount", required = false) String minAmount,
            @RequestParam(name = "maxAmount", required = false) String maxAmount,
            @RequestParam(name = "sort", required = false) String sort
    ) {
        var customerId = currentCustomerId(authentication);
        var query = queryParser.parse(accountId, status, direction, type, category, merchant, search,
                dateFrom, dateTo, minAmount, maxAmount, 0, 100, sort);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + csvExporter.filename(customerId) + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvExporter.export(customerId, query));
    }

    private UUID currentCustomerId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
