package com.novabank.account.consumer;

import com.novabank.account.domain.ProcessedEvent;
import com.novabank.account.event.AccountEventPublisher;
import com.novabank.account.exception.AccountException;
import com.novabank.account.repository.AccountBalanceRepository;
import com.novabank.account.repository.AccountRepository;
import com.novabank.account.repository.ProcessedEventRepository;
import com.novabank.shared.events.BankingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class TransactionEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final AccountEventPublisher eventPublisher;

    public TransactionEventConsumer(
            AccountRepository accountRepository,
            AccountBalanceRepository balanceRepository,
            ProcessedEventRepository processedEventRepository,
            AccountEventPublisher eventPublisher
    ) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.processedEventRepository = processedEventRepository;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = {"transaction.created", "transaction.posted"})
    @Transactional
    public void onTransactionEvent(BankingEvent event) {
        if (processedEventRepository.existsById(event.eventId())) {
            return;
        }
        try {
            processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), event.eventType(), event.aggregateId()));
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Duplicate transaction event delivery ignored eventId={}", event.eventId());
            return;
        }
        apply(event);
    }

    private void apply(BankingEvent event) {
        var payload = event.payload();
        var accountId = uuid(payload, "accountId");
        var account = accountRepository.findByIdAndCustomerId(accountId, event.customerId())
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
        var balance = balanceRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_BALANCE_NOT_FOUND", "Account balance was not found"));

        var amount = amount(payload);
        var status = string(payload, "status");
        var previousStatus = payload.get("previousStatus") == null ? null : String.valueOf(payload.get("previousStatus"));
        var direction = string(payload, "direction");

        if ("TransactionCreated".equals(event.eventType())) {
            if ("PENDING".equals(status)) {
                applyPending(balance, direction, amount);
            } else if ("POSTED".equals(status)) {
                applyPostedCreated(balance, direction, amount);
            }
        } else if ("TransactionPosted".equals(event.eventType())) {
            if (!"PENDING".equals(previousStatus)) {
                return;
            }
            applyPendingToPosted(balance, direction, amount);
        }

        balance.setAsOf(Instant.now());
        balanceRepository.save(balance);
        eventPublisher.publish("balance.updated", BankingEvent.v1(
                "BalanceUpdated",
                event.correlationId(),
                event.customerId(),
                account.getId(),
                Map.of(
                        "accountId", account.getId().toString(),
                        "currentBalance", balance.getCurrentBalance().toPlainString(),
                        "availableBalance", balance.getAvailableBalance().toPlainString(),
                        "pendingDebitAmount", balance.getPendingDebitAmount().toPlainString(),
                        "pendingCreditAmount", balance.getPendingCreditAmount().toPlainString()
                )
        ));
    }

    private void applyPending(com.novabank.account.domain.AccountBalance balance, String direction, BigDecimal amount) {
        if ("DEBIT".equals(direction)) {
            balance.setPendingDebitAmount(balance.getPendingDebitAmount().add(amount));
            balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        } else if ("CREDIT".equals(direction)) {
            balance.setPendingCreditAmount(balance.getPendingCreditAmount().add(amount));
        }
    }

    private void applyPostedCreated(com.novabank.account.domain.AccountBalance balance, String direction, BigDecimal amount) {
        if ("DEBIT".equals(direction)) {
            balance.setCurrentBalance(balance.getCurrentBalance().subtract(amount));
            balance.setAvailableBalance(balance.getAvailableBalance().subtract(amount));
        } else if ("CREDIT".equals(direction)) {
            balance.setCurrentBalance(balance.getCurrentBalance().add(amount));
            balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
        }
    }

    private void applyPendingToPosted(com.novabank.account.domain.AccountBalance balance, String direction, BigDecimal amount) {
        if ("DEBIT".equals(direction)) {
            balance.setCurrentBalance(balance.getCurrentBalance().subtract(amount));
            balance.setPendingDebitAmount(nonNegative(balance.getPendingDebitAmount().subtract(amount), "pending debit"));
        } else if ("CREDIT".equals(direction)) {
            balance.setCurrentBalance(balance.getCurrentBalance().add(amount));
            balance.setAvailableBalance(balance.getAvailableBalance().add(amount));
            balance.setPendingCreditAmount(nonNegative(balance.getPendingCreditAmount().subtract(amount), "pending credit"));
        }
    }

    private BigDecimal nonNegative(BigDecimal value, String label) {
        if (value.signum() < 0) {
            throw new AccountException(HttpStatus.CONFLICT, "NEGATIVE_PENDING_TOTAL", "Transaction event would make " + label + " total negative");
        }
        return value;
    }

    private UUID uuid(Map<String, Object> payload, String key) {
        return UUID.fromString(string(payload, key));
    }

    private String string(Map<String, Object> payload, String key) {
        var value = payload.get(key);
        if (value == null) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_EVENT", "Transaction event is missing " + key);
        }
        return String.valueOf(value);
    }

    private BigDecimal amount(Map<String, Object> payload) {
        var amount = new BigDecimal(string(payload, "amount"));
        if (amount.signum() <= 0) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_EVENT", "Transaction event amount must be positive");
        }
        return amount;
    }
}
