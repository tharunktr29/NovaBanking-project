package com.novabank.payment.controller;

import com.novabank.payment.dto.request.CreatePayeeRequest;
import com.novabank.payment.dto.request.UpdatePayeeRequest;
import com.novabank.payment.dto.response.PayeeResponse;
import com.novabank.payment.service.IdempotencyService;
import com.novabank.payment.service.PayeeService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payees")
public class PayeeController {
    private final PayeeService payeeService;
    private final IdempotencyService idempotencyService;

    public PayeeController(PayeeService payeeService, IdempotencyService idempotencyService) {
        this.payeeService = payeeService;
        this.idempotencyService = idempotencyService;
    }

    @GetMapping
    List<PayeeResponse> list(@AuthenticationPrincipal String customerId) {
        return payeeService.list(UUID.fromString(customerId));
    }

    @PostMapping
    PayeeResponse create(
            @AuthenticationPrincipal String customerId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CreatePayeeRequest request
    ) {
        var customer = UUID.fromString(customerId);
        return idempotencyService.run(customer, "CREATE_PAYEE", idempotencyKey, request, PayeeResponse.class,
                () -> payeeService.create(customer, request));
    }

    @PutMapping("/{payeeId}")
    PayeeResponse update(@AuthenticationPrincipal String customerId, @PathVariable UUID payeeId, @Valid @RequestBody UpdatePayeeRequest request) {
        return payeeService.update(UUID.fromString(customerId), payeeId, request);
    }

    @DeleteMapping("/{payeeId}")
    void delete(@AuthenticationPrincipal String customerId, @PathVariable UUID payeeId) {
        payeeService.disable(UUID.fromString(customerId), payeeId);
    }
}
