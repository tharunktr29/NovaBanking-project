package com.novabank.card.controller;

import com.novabank.card.dto.PageResponse;
import com.novabank.card.dto.request.ReplacementRequestDto;
import com.novabank.card.dto.request.UpdateCardControlsRequest;
import com.novabank.card.dto.response.CardControlsResponse;
import com.novabank.card.dto.response.CardHistoryResponse;
import com.novabank.card.dto.response.CardResponse;
import com.novabank.card.dto.response.ReplacementRequestResponse;
import com.novabank.card.service.CardQueryParser;
import com.novabank.card.service.CardService;
import com.novabank.shared.correlation.Correlation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {
    private final CardService cardService;
    private final CardQueryParser queryParser;

    public CardController(CardService cardService, CardQueryParser queryParser) {
        this.cardService = cardService;
        this.queryParser = queryParser;
    }

    @GetMapping
    PageResponse<CardResponse> cards(
            Authentication authentication,
            @RequestParam(name = "cardType", required = false) String cardType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "accountId", required = false) String accountId,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "size", required = false) Integer size,
            @RequestParam(name = "sort", required = false) String sort,
            HttpServletRequest request
    ) {
        return cardService.list(currentCustomerId(authentication), queryParser.parse(cardType, status, accountId, page, size, sort), authorization(request));
    }

    @GetMapping("/{cardId}")
    CardResponse card(Authentication authentication, @PathVariable UUID cardId, HttpServletRequest request) {
        return cardService.detail(currentCustomerId(authentication), cardId, authorization(request));
    }

    @GetMapping("/{cardId}/history")
    List<CardHistoryResponse> history(Authentication authentication, @PathVariable UUID cardId) {
        return cardService.history(currentCustomerId(authentication), cardId);
    }

    @PostMapping("/{cardId}/activate")
    CardResponse activate(
            Authentication authentication,
            @PathVariable UUID cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request
    ) {
        return cardService.activate(currentCustomerId(authentication), cardId, idempotencyKey, correlation(request), authorization(request));
    }

    @PostMapping("/{cardId}/lock")
    CardResponse lock(
            Authentication authentication,
            @PathVariable UUID cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request
    ) {
        return cardService.lock(currentCustomerId(authentication), cardId, idempotencyKey, correlation(request), authorization(request));
    }

    @PostMapping("/{cardId}/unlock")
    CardResponse unlock(
            Authentication authentication,
            @PathVariable UUID cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            HttpServletRequest request
    ) {
        return cardService.unlock(currentCustomerId(authentication), cardId, idempotencyKey, correlation(request), authorization(request));
    }

    @PostMapping("/{cardId}/replacement-requests")
    ReplacementRequestResponse replacement(
            Authentication authentication,
            @PathVariable UUID cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReplacementRequestDto replacementRequest,
            HttpServletRequest request
    ) {
        return cardService.requestReplacement(currentCustomerId(authentication), cardId, replacementRequest, idempotencyKey, correlation(request));
    }

    @GetMapping("/{cardId}/controls")
    CardControlsResponse controls(Authentication authentication, @PathVariable UUID cardId) {
        return cardService.controls(currentCustomerId(authentication), cardId);
    }

    @PutMapping("/{cardId}/controls")
    CardControlsResponse updateControls(
            Authentication authentication,
            @PathVariable UUID cardId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody UpdateCardControlsRequest controlsRequest,
            HttpServletRequest request
    ) {
        return cardService.updateControls(currentCustomerId(authentication), cardId, controlsRequest, idempotencyKey, correlation(request));
    }

    private UUID currentCustomerId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private String correlation(HttpServletRequest request) {
        return request.getHeader(Correlation.HEADER_NAME);
    }

    private String authorization(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.AUTHORIZATION);
    }
}
