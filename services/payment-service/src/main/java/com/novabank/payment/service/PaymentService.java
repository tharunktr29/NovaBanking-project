package com.novabank.payment.service;

import com.novabank.payment.domain.*;
import com.novabank.payment.dto.PageResponse;
import com.novabank.payment.dto.internal.ApplyCreditCardPaymentRequest;
import com.novabank.payment.dto.internal.HistoryTransactionRequest;
import com.novabank.payment.dto.internal.PostLedgerTransactionRequest;
import com.novabank.payment.dto.request.CreditCardPaymentRequest;
import com.novabank.payment.dto.request.ExternalPaymentRequest;
import com.novabank.payment.dto.request.InternalTransferRequest;
import com.novabank.payment.dto.response.PaymentHistoryResponse;
import com.novabank.payment.dto.response.PaymentResponse;
import com.novabank.payment.exception.PaymentException;
import com.novabank.payment.mapper.PaymentMapper;
import com.novabank.payment.repository.PaymentOrderRepository;
import com.novabank.payment.repository.PaymentStatusHistoryRepository;
import com.novabank.payment.security.JwtService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    private final PaymentOrderRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final PaymentMapper mapper;
    private final AccountClient accountClient;
    private final CardClient cardClient;
    private final TransactionHistoryClient transactionHistoryClient;
    private final PayeeService payeeService;
    private final OutboxService outboxService;
    private final JwtService jwtService;

    public PaymentService(
            PaymentOrderRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentMapper mapper,
            AccountClient accountClient,
            CardClient cardClient,
            TransactionHistoryClient transactionHistoryClient,
            PayeeService payeeService,
            OutboxService outboxService,
            JwtService jwtService
    ) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.mapper = mapper;
        this.accountClient = accountClient;
        this.cardClient = cardClient;
        this.transactionHistoryClient = transactionHistoryClient;
        this.payeeService = payeeService;
        this.outboxService = outboxService;
        this.jwtService = jwtService;
    }

    @Transactional
    public PaymentResponse createInternalTransfer(UUID customerId, InternalTransferRequest request, String authHeader, UUID correlationId) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER", "Source and destination accounts must be different");
        }
        validateSchedule(request.executionType(), request.scheduledFor());
        accountClient.getAccount(request.sourceAccountId(), authHeader);
        accountClient.getAccount(request.destinationAccountId(), authHeader);
        var order = newOrder(customerId, PaymentType.INTERNAL_TRANSFER, request.sourceAccountId(), request.destinationAccountId(),
                null, null, request.amount(), request.currency(), request.memo(), request.executionType(), request.scheduledFor(), correlationId);
        return saveAndMaybeProcess(order, authHeader);
    }

    @Transactional
    public PaymentResponse createCreditCardPayment(UUID customerId, CreditCardPaymentRequest request, String authHeader, UUID correlationId) {
        validateSchedule(request.executionType(), request.scheduledFor());
        accountClient.getAccount(request.sourceAccountId(), authHeader);
        var card = cardClient.getCard(request.destinationCardId(), authHeader);
        if (!"CREDIT".equals(card.cardType())) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "NOT_CREDIT_CARD", "Destination card must be a credit card");
        }
        var order = newOrder(customerId, PaymentType.CREDIT_CARD_PAYMENT, request.sourceAccountId(), null,
                request.destinationCardId(), null, request.amount(), request.currency(), request.memo(), request.executionType(), request.scheduledFor(), correlationId);
        return saveAndMaybeProcess(order, authHeader);
    }

    @Transactional
    public PaymentResponse createExternalPayment(UUID customerId, ExternalPaymentRequest request, String authHeader, UUID correlationId) {
        validateSchedule(request.executionType(), request.scheduledFor());
        accountClient.getAccount(request.sourceAccountId(), authHeader);
        payeeService.requireVerified(customerId, request.externalPayeeId());
        var order = newOrder(customerId, PaymentType.EXTERNAL_ACCOUNT_PAYMENT, request.sourceAccountId(), null,
                null, request.externalPayeeId(), request.amount(), request.currency(), request.memo(), request.executionType(), request.scheduledFor(), correlationId);
        return saveAndMaybeProcess(order, authHeader);
    }

    public PageResponse<PaymentResponse> list(UUID customerId, PaymentQuery query, int page, int size, String sort) {
        var safePage = Math.max(0, page);
        var safeSize = Math.min(50, Math.max(1, size));
        var direction = sort != null && sort.endsWith(",asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        var sortProperty = "createdAt";
        if (sort != null && sort.startsWith("scheduledFor")) sortProperty = "scheduledFor";
        if (sort != null && sort.startsWith("amount")) sortProperty = "amount";
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(direction, sortProperty));
        var result = paymentRepository.findAll((root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("customerId"), customerId));
            if (query.type() != null) predicates.add(cb.equal(root.get("paymentType"), query.type()));
            if (query.status() != null) predicates.add(cb.equal(root.get("status"), query.status()));
            if (query.from() != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), query.from()));
            if (query.to() != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), query.to()));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable);
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    public PaymentResponse get(UUID customerId, UUID paymentId) {
        return mapper.toResponse(paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found")));
    }

    public List<PaymentHistoryResponse> history(UUID customerId, UUID paymentId) {
        var order = paymentRepository.findByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found"));
        return historyRepository.findByPaymentIdOrderByOccurredAtAsc(order.getId()).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public PaymentResponse cancel(UUID customerId, UUID paymentId, UUID correlationId) {
        var order = paymentRepository.findWithLockByIdAndCustomerId(paymentId, customerId)
                .orElseThrow(() -> new PaymentException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found"));
        if (order.getStatus() != PaymentStatus.SCHEDULED) {
            throw new PaymentException(HttpStatus.CONFLICT, "PAYMENT_NOT_CANCELLABLE", "Only scheduled payments can be cancelled");
        }
        transition(order, PaymentStatus.CANCELLED, "CUSTOMER_CANCELLED", correlationId);
        outboxService.add(order, "PaymentCancelled");
        return mapper.toResponse(order);
    }

    @Scheduled(fixedDelayString = "${PAYMENT_SCHEDULER_DELAY_MS:15000}")
    @Transactional
    public void processDueScheduledPayments() {
        paymentRepository.findTop25ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(PaymentStatus.SCHEDULED, Instant.now())
                .forEach(order -> {
                    try {
                        var authHeader = "Bearer " + jwtService.createInternalAccessToken(order.getCustomerId());
                        process(order, authHeader);
                    } catch (Exception ex) {
                        fail(order, PaymentFailureCode.PROCESSING_ERROR, "Scheduled payment failed", order.getCorrelationId());
                    }
                });
    }

    private PaymentResponse saveAndMaybeProcess(PaymentOrder order, String authHeader) {
        paymentRepository.save(order);
        history(order, null, order.getStatus(), "CREATED", order.getCorrelationId());
        outboxService.add(order, order.getStatus() == PaymentStatus.SCHEDULED ? "PaymentScheduled" : "PaymentProcessing");
        if (order.getExecutionType() == ExecutionType.IMMEDIATE) {
            process(order, authHeader);
        }
        return mapper.toResponse(order);
    }

    private void process(PaymentOrder order, String authHeader) {
        if (order.getStatus() == PaymentStatus.SCHEDULED) {
            transition(order, PaymentStatus.PENDING, "SCHEDULE_DUE", order.getCorrelationId());
        }
        transition(order, PaymentStatus.PROCESSING, "PROCESSING_STARTED", order.getCorrelationId());
        try {
            accountClient.postLedger(new PostLedgerTransactionRequest(
                    order.getId(),
                    switch (order.getPaymentType()) {
                        case INTERNAL_TRANSFER -> "INTERNAL_TRANSFER";
                        case CREDIT_CARD_PAYMENT -> "CREDIT_CARD_PAYMENT";
                        case EXTERNAL_ACCOUNT_PAYMENT -> "EXTERNAL_PAYMENT";
                    },
                    order.getSourceAccountId(),
                    order.getDestinationAccountId(),
                    order.getAmount(),
                    order.getCurrency(),
                    order.getPaymentReference()
            ), authHeader == null ? "" : authHeader, order.getCorrelationId().toString());

            if (order.getPaymentType() == PaymentType.CREDIT_CARD_PAYMENT) {
                cardClient.applyPayment(new ApplyCreditCardPaymentRequest(order.getId(), order.getDestinationCardId(), order.getAmount(), order.getCurrency()),
                        authHeader == null ? "" : authHeader, order.getCorrelationId().toString());
            }
            recordHistoryProjection(order, authHeader);
            transition(order, PaymentStatus.COMPLETED, "COMPLETED", order.getCorrelationId());
            order.setCompletedAt(Instant.now());
            outboxService.add(order, eventForCompletion(order));
        } catch (PaymentException ex) {
            fail(order, failureCode(ex.code()), ex.getMessage(), order.getCorrelationId());
            throw ex;
        }
    }

    private void recordHistoryProjection(PaymentOrder order, String authHeader) {
        var now = Instant.now();
        var description = switch (order.getPaymentType()) {
            case INTERNAL_TRANSFER -> "Internal transfer";
            case CREDIT_CARD_PAYMENT -> "Credit card payment";
            case EXTERNAL_ACCOUNT_PAYMENT -> "External payment";
        };
        transactionHistoryClient.record(new HistoryTransactionRequest(
                "payment:" + order.getId() + ":debit",
                order.getSourceAccountId(),
                "DEBIT",
                "PAYMENT",
                order.getPaymentType().name(),
                description,
                order.getAmount(),
                order.getCurrency(),
                now
        ), authHeader == null ? "" : authHeader, order.getCorrelationId().toString());
        if (order.getPaymentType() == PaymentType.INTERNAL_TRANSFER && order.getDestinationAccountId() != null) {
            transactionHistoryClient.record(new HistoryTransactionRequest(
                    "payment:" + order.getId() + ":credit",
                    order.getDestinationAccountId(),
                    "CREDIT",
                    "TRANSFER",
                    order.getPaymentType().name(),
                    "Internal transfer received",
                    order.getAmount(),
                    order.getCurrency(),
                    now
            ), authHeader == null ? "" : authHeader, order.getCorrelationId().toString());
        }
    }

    private PaymentOrder newOrder(
            UUID customerId,
            PaymentType type,
            UUID sourceAccountId,
            UUID destinationAccountId,
            UUID destinationCardId,
            UUID externalPayeeId,
            BigDecimal amount,
            String currency,
            String memo,
            ExecutionType executionType,
            Instant scheduledFor,
            UUID correlationId
    ) {
        var order = new PaymentOrder();
        order.setPaymentReference("PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCustomerId(customerId);
        order.setPaymentType(type);
        order.setStatus(executionType == ExecutionType.SCHEDULED ? PaymentStatus.SCHEDULED : PaymentStatus.PENDING);
        order.setSourceAccountId(sourceAccountId);
        order.setDestinationAccountId(destinationAccountId);
        order.setDestinationCardId(destinationCardId);
        order.setExternalPayeeId(externalPayeeId);
        order.setAmount(amount);
        order.setCurrency(currency);
        order.setMemo(cleanMemo(memo));
        order.setExecutionType(executionType);
        order.setScheduledFor(scheduledFor);
        order.setCorrelationId(correlationId);
        return order;
    }

    private void validateSchedule(ExecutionType executionType, Instant scheduledFor) {
        if (executionType == ExecutionType.IMMEDIATE && scheduledFor != null) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "SCHEDULE_NOT_ALLOWED", "Immediate payments cannot include scheduledFor");
        }
        if (executionType == ExecutionType.SCHEDULED && (scheduledFor == null || scheduledFor.isBefore(Instant.now().plusSeconds(60)))) {
            throw new PaymentException(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE", "Scheduled payments must be at least one minute in the future");
        }
    }

    private void transition(PaymentOrder order, PaymentStatus newStatus, String reason, UUID correlationId) {
        var previous = order.getStatus();
        order.setStatus(newStatus);
        if (newStatus == PaymentStatus.PROCESSING) order.setProcessingStartedAt(Instant.now());
        history(order, previous, newStatus, reason, correlationId);
    }

    private void fail(PaymentOrder order, PaymentFailureCode code, String message, UUID correlationId) {
        var previous = order.getStatus();
        order.setStatus(PaymentStatus.FAILED);
        order.setFailedAt(Instant.now());
        order.setFailureCode(code);
        order.setFailureMessage(message == null ? "Payment failed" : message.replaceAll("[\\r\\n\\t]", " "));
        history(order, previous, PaymentStatus.FAILED, code.name(), correlationId);
        outboxService.add(order, "PaymentFailed");
    }

    private void history(PaymentOrder order, PaymentStatus previous, PaymentStatus next, String reason, UUID correlationId) {
        var history = new PaymentStatusHistory();
        history.setPaymentId(order.getId());
        history.setPreviousStatus(previous);
        history.setNewStatus(next);
        history.setReasonCode(reason);
        history.setEventId(UUID.randomUUID());
        history.setCorrelationId(correlationId);
        historyRepository.save(history);
    }

    private String eventForCompletion(PaymentOrder order) {
        return switch (order.getPaymentType()) {
            case INTERNAL_TRANSFER -> "InternalTransferCompleted";
            case CREDIT_CARD_PAYMENT -> "CreditCardPaymentApplied";
            case EXTERNAL_ACCOUNT_PAYMENT -> "ExternalPaymentCompleted";
        };
    }

    private PaymentFailureCode failureCode(String code) {
        if ("LEDGER_REJECTED".equals(code)) return PaymentFailureCode.INSUFFICIENT_FUNDS;
        if ("PAYEE_NOT_VERIFIED".equals(code)) return PaymentFailureCode.PAYEE_NOT_VERIFIED;
        if ("CARD_PAYMENT_REJECTED".equals(code)) return PaymentFailureCode.CARD_NOT_ELIGIBLE;
        return PaymentFailureCode.PROCESSING_ERROR;
    }

    private String cleanMemo(String memo) {
        if (memo == null || memo.isBlank()) return null;
        return memo.trim().replaceAll("[\\r\\n\\t]", " ").replaceAll("\\s+", " ");
    }
}
