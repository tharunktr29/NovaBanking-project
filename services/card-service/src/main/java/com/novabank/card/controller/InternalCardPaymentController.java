package com.novabank.card.controller;

import com.novabank.card.dto.request.ApplyCreditCardPaymentRequest;
import com.novabank.card.dto.response.ApplyCreditCardPaymentResponse;
import com.novabank.card.service.CardPaymentService;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/cards")
public class InternalCardPaymentController {
    private final CardPaymentService cardPaymentService;

    public InternalCardPaymentController(CardPaymentService cardPaymentService) {
        this.cardPaymentService = cardPaymentService;
    }

    @PostMapping("/credit-payments")
    ApplyCreditCardPaymentResponse apply(Authentication authentication, @Valid @RequestBody ApplyCreditCardPaymentRequest request, HttpServletRequest servletRequest) {
        return cardPaymentService.apply(UUID.fromString(authentication.getName()), request, servletRequest.getHeader(Correlation.HEADER_NAME));
    }
}
