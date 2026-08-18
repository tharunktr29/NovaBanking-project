package com.novabank.card.service;

import com.novabank.card.domain.*;
import com.novabank.card.dto.PageResponse;
import com.novabank.card.dto.request.ReplacementRequestDto;
import com.novabank.card.dto.request.UpdateCardControlsRequest;
import com.novabank.card.dto.response.CardControlsResponse;
import com.novabank.card.dto.response.CardHistoryResponse;
import com.novabank.card.dto.response.CardResponse;
import com.novabank.card.dto.response.ReplacementRequestResponse;
import com.novabank.card.exception.CardException;
import com.novabank.card.mapper.CardMapper;
import com.novabank.card.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CardService {
    private final BankCardRepository cardRepository;
    private final CardControlsRepository controlsRepository;
    private final CreditDetailsRepository creditDetailsRepository;
    private final CardLifecycleHistoryRepository historyRepository;
    private final ReplacementRequestRepository replacementRepository;
    private final CardMapper mapper;
    private final AccountClient accountClient;
    private final OutboxService outboxService;
    private final IdempotencyService idempotencyService;

    public CardService(
            BankCardRepository cardRepository,
            CardControlsRepository controlsRepository,
            CreditDetailsRepository creditDetailsRepository,
            CardLifecycleHistoryRepository historyRepository,
            ReplacementRequestRepository replacementRepository,
            CardMapper mapper,
            AccountClient accountClient,
            OutboxService outboxService,
            IdempotencyService idempotencyService
    ) {
        this.cardRepository = cardRepository;
        this.controlsRepository = controlsRepository;
        this.creditDetailsRepository = creditDetailsRepository;
        this.historyRepository = historyRepository;
        this.replacementRepository = replacementRepository;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.outboxService = outboxService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional(readOnly = true)
    public PageResponse<CardResponse> list(UUID customerId, CardQuery query, String authorizationHeader) {
        var page = cardRepository.findAll(specification(customerId, query), PageRequest.of(query.page(), query.size(), query.sort()));
        var responses = page.getContent().stream()
                .map(card -> toResponse(card, authorizationHeader))
                .toList();
        return new PageResponse<>(
                responses,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                query.sortText()
        );
    }

    @Transactional(readOnly = true)
    public CardResponse detail(UUID customerId, UUID cardId, String authorizationHeader) {
        return toResponse(ownedCard(customerId, cardId), authorizationHeader);
    }

    @Transactional(readOnly = true)
    public List<CardHistoryResponse> history(UUID customerId, UUID cardId) {
        var card = ownedCard(customerId, cardId);
        return historyRepository.findByCardIdOrderByOccurredAtDesc(card.getId()).stream()
                .map(mapper::toHistoryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardControlsResponse controls(UUID customerId, UUID cardId) {
        var card = ownedCard(customerId, cardId);
        return mapper.toControlsResponse(controlsFor(card));
    }

    @Transactional
    public CardResponse activate(UUID customerId, UUID cardId, String idempotencyKey, String correlationId, String authorizationHeader) {
        return idempotencyService.run(customerId, cardId, "ACTIVATE_CARD", idempotencyKey, null, CardResponse.class,
                () -> changeStatus(customerId, cardId, CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE, CardAction.ACTIVATED, null, correlationId, authorizationHeader));
    }

    @Transactional
    public CardResponse lock(UUID customerId, UUID cardId, String idempotencyKey, String correlationId, String authorizationHeader) {
        return idempotencyService.run(customerId, cardId, "LOCK_CARD", idempotencyKey, null, CardResponse.class,
                () -> changeStatus(customerId, cardId, CardStatus.ACTIVE, CardStatus.LOCKED, CardAction.LOCKED, null, correlationId, authorizationHeader));
    }

    @Transactional
    public CardResponse unlock(UUID customerId, UUID cardId, String idempotencyKey, String correlationId, String authorizationHeader) {
        return idempotencyService.run(customerId, cardId, "UNLOCK_CARD", idempotencyKey, null, CardResponse.class,
                () -> changeStatus(customerId, cardId, CardStatus.LOCKED, CardStatus.ACTIVE, CardAction.UNLOCKED, null, correlationId, authorizationHeader));
    }

    @Transactional
    public ReplacementRequestResponse requestReplacement(
            UUID customerId,
            UUID cardId,
            ReplacementRequestDto request,
            String idempotencyKey,
            String correlationId
    ) {
        return idempotencyService.run(customerId, cardId, "REQUEST_CARD_REPLACEMENT", idempotencyKey, request, ReplacementRequestResponse.class,
                () -> createReplacement(customerId, cardId, request, correlationId));
    }

    @Transactional
    public CardControlsResponse updateControls(
            UUID customerId,
            UUID cardId,
            UpdateCardControlsRequest request,
            String idempotencyKey,
            String correlationId
    ) {
        return idempotencyService.run(customerId, cardId, "UPDATE_CARD_CONTROLS", idempotencyKey, request, CardControlsResponse.class,
                () -> changeControls(customerId, cardId, request, correlationId));
    }

    private CardResponse changeStatus(
            UUID customerId,
            UUID cardId,
            CardStatus required,
            CardStatus next,
            CardAction action,
            String reason,
            String correlationId,
            String authorizationHeader
    ) {
        var card = ownedCard(customerId, cardId);
        var previous = card.getStatus();
        if (previous != required) {
            throw new CardException(HttpStatus.CONFLICT, "INVALID_CARD_STATE", "Card is not in a state that allows this action");
        }
        card.setStatus(next);
        var now = Instant.now();
        if (action == CardAction.ACTIVATED) {
            card.setActivatedAt(now);
        }
        if (action == CardAction.LOCKED) {
            card.setLockedAt(now);
        }
        if (action == CardAction.UNLOCKED) {
            card.setLockedAt(null);
        }
        var saved = cardRepository.save(card);
        addHistory(saved, action, previous, saved.getStatus(), reason, correlationId, now);
        outboxService.add(saved, action, previous, normalizeCorrelation(correlationId).toString(), now);
        return toResponse(saved, authorizationHeader);
    }

    private ReplacementRequestResponse createReplacement(UUID customerId, UUID cardId, ReplacementRequestDto request, String correlationId) {
        var card = ownedCard(customerId, cardId);
        if (card.getStatus() == CardStatus.CLOSED) {
            throw new CardException(HttpStatus.CONFLICT, "CARD_CLOSED", "Closed cards cannot be replaced");
        }
        if (replacementRepository.existsByCardIdAndStatusIn(cardId, List.of(ReplacementRequestStatus.REQUESTED, ReplacementRequestStatus.PROCESSING))) {
            throw new CardException(HttpStatus.CONFLICT, "REPLACEMENT_ALREADY_ACTIVE", "A replacement request already exists for this card");
        }
        var previous = card.getStatus();
        card.setStatus(CardStatus.REPLACEMENT_REQUESTED);
        var saved = cardRepository.save(card);
        var replacement = new ReplacementRequest();
        replacement.setCardId(saved.getId());
        replacement.setReason(request.reason());
        replacement.setRequestReference("RPL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        replacement = replacementRepository.save(replacement);
        var now = Instant.now();
        addHistory(saved, CardAction.REPLACEMENT_REQUESTED, previous, saved.getStatus(), request.reason().name(), correlationId, now);
        outboxService.add(saved, CardAction.REPLACEMENT_REQUESTED, previous, normalizeCorrelation(correlationId).toString(), now);
        return new ReplacementRequestResponse(saved.getId(), replacement.getRequestReference(), replacement.getStatus(), replacement.getRequestedAt(), "Replacement request received");
    }

    private CardControlsResponse changeControls(UUID customerId, UUID cardId, UpdateCardControlsRequest request, String correlationId) {
        var card = ownedCard(customerId, cardId);
        if (card.getStatus() == CardStatus.CLOSED || card.getStatus() == CardStatus.EXPIRED) {
            throw new CardException(HttpStatus.CONFLICT, "CARD_CONTROLS_LOCKED", "Spending controls cannot be changed for closed or expired cards");
        }
        if (!request.atmWithdrawalsEnabled() && request.dailyAtmLimit().compareTo(BigDecimal.ZERO) > 0) {
            throw new CardException(HttpStatus.BAD_REQUEST, "INVALID_ATM_LIMIT", "Daily ATM limit must be zero when ATM withdrawals are disabled");
        }
        var controls = controlsFor(card);
        controls.setOnlinePurchasesEnabled(request.onlinePurchasesEnabled());
        controls.setContactlessEnabled(request.contactlessEnabled());
        controls.setInternationalPurchasesEnabled(request.internationalPurchasesEnabled());
        controls.setAtmWithdrawalsEnabled(request.atmWithdrawalsEnabled());
        controls.setDailyPurchaseLimit(request.dailyPurchaseLimit());
        controls.setDailyAtmLimit(request.dailyAtmLimit());
        controls.setCurrency(request.currency());
        var saved = controlsRepository.save(controls);
        var now = Instant.now();
        addHistory(card, CardAction.CONTROLS_UPDATED, card.getStatus(), card.getStatus(), null, correlationId, now);
        outboxService.add(card, CardAction.CONTROLS_UPDATED, card.getStatus(), normalizeCorrelation(correlationId).toString(), now);
        return mapper.toControlsResponse(saved);
    }

    private CardResponse toResponse(BankCard card, String authorizationHeader) {
        var controls = controlsRepository.findByCardId(card.getId()).orElse(null);
        var credit = creditDetailsRepository.findByCardId(card.getId());
        var account = accountClient.getAccount(card.getAccountId(), authorizationHeader);
        return mapper.toResponse(card, controls, credit, account);
    }

    private CardControls controlsFor(BankCard card) {
        return controlsRepository.findByCardId(card.getId())
                .orElseThrow(() -> new CardException(HttpStatus.NOT_FOUND, "CARD_CONTROLS_NOT_FOUND", "Card controls were not found"));
    }

    private BankCard ownedCard(UUID customerId, UUID cardId) {
        return cardRepository.findByIdAndCustomerId(cardId, customerId)
                .orElseThrow(() -> new CardException(HttpStatus.NOT_FOUND, "CARD_NOT_FOUND", "Card was not found"));
    }

    private Specification<BankCard> specification(UUID customerId, CardQuery query) {
        return (root, criteriaQuery, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("customerId"), customerId));
            if (query.cardType() != null) {
                predicates.add(builder.equal(root.get("cardType"), query.cardType()));
            }
            if (query.status() != null) {
                predicates.add(builder.equal(root.get("status"), query.status()));
            }
            if (query.accountId() != null) {
                predicates.add(builder.equal(root.get("accountId"), query.accountId()));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private void addHistory(BankCard card, CardAction action, CardStatus previous, CardStatus next, String reason, String correlationId, Instant occurredAt) {
        var history = new CardLifecycleHistory();
        history.setCardId(card.getId());
        history.setAction(action);
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setReasonCode(reason);
        history.setCorrelationId(normalizeCorrelation(correlationId));
        history.setOccurredAt(occurredAt);
        historyRepository.save(history);
    }

    private UUID normalizeCorrelation(String correlationId) {
        try {
            return UUID.fromString(correlationId);
        } catch (RuntimeException ex) {
            return UUID.randomUUID();
        }
    }
}
