package com.novabank.transaction.controller;

import com.novabank.shared.correlation.Correlation;
import com.novabank.transaction.domain.TransactionStatus;
import com.novabank.transaction.dto.DevTransactionRequest;
import com.novabank.transaction.dto.IngestTransactionRequest;
import com.novabank.transaction.dto.TransactionResponse;
import com.novabank.transaction.exception.TransactionException;
import com.novabank.transaction.service.TransactionIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Profile("local")
@ConditionalOnProperty(prefix = "novabank.dev-tools", name = "enabled", havingValue = "true")
@RestController
@RequestMapping("/api/dev/transactions")
public class DevTransactionController {
    private static final UUID DEMO_CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final TransactionIngestionService ingestionService;

    public DevTransactionController(TransactionIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionResponse create(Authentication authentication, @Valid @RequestBody DevTransactionRequest request, HttpServletRequest servletRequest) {
        var now = Instant.now();
        var status = request.status() == null ? TransactionStatus.PENDING : request.status();
        return ingestionService.ingest(new IngestTransactionRequest(
                "dev-" + UUID.randomUUID(),
                currentCustomerId(authentication),
                request.accountId(),
                request.merchantId(),
                status,
                request.direction(),
                request.type(),
                request.category(),
                request.description(),
                request.amount(),
                request.currency() == null ? "USD" : request.currency(),
                now,
                status == TransactionStatus.POSTED ? now : null
        ), servletRequest.getHeader(Correlation.HEADER_NAME));
    }

    @PostMapping("/{transactionId}/post")
    TransactionResponse post(Authentication authentication, @PathVariable UUID transactionId, HttpServletRequest servletRequest) {
        return ingestionService.post(currentCustomerId(authentication), transactionId, servletRequest.getHeader(Correlation.HEADER_NAME));
    }

    private UUID currentCustomerId(Authentication authentication) {
        var customerId = UUID.fromString(authentication.getName());
        if (!DEMO_CUSTOMER_ID.equals(customerId)) {
            throw new TransactionException(HttpStatus.FORBIDDEN, "DEV_SIMULATOR_DEMO_ONLY", "Development simulator is limited to the demo customer");
        }
        return customerId;
    }
}
