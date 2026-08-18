package com.novabank.account.service;

import com.novabank.account.domain.*;
import com.novabank.account.dto.request.PostLedgerTransactionRequest;
import com.novabank.account.dto.response.LedgerEntryResponse;
import com.novabank.account.dto.response.LedgerTransactionResponse;
import com.novabank.account.exception.AccountException;
import com.novabank.account.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {
    public static final UUID CREDIT_CARD_CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-00000000cc01");
    public static final UUID EXTERNAL_CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-00000000ee01");
    public static final UUID DISPUTE_CLEARING_ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-00000000dd01");

    private final AccountRepository accountRepository;
    private final AccountBalanceRepository balanceRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(
            AccountRepository accountRepository,
            AccountBalanceRepository balanceRepository,
            LedgerTransactionRepository ledgerTransactionRepository,
            LedgerEntryRepository ledgerEntryRepository
    ) {
        this.accountRepository = accountRepository;
        this.balanceRepository = balanceRepository;
        this.ledgerTransactionRepository = ledgerTransactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    @Transactional
    public LedgerTransactionResponse post(UUID customerId, PostLedgerTransactionRequest request, String correlationId) {
        var existing = ledgerTransactionRepository.findByBusinessOperationId(request.businessOperationId());
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }
        validate(request);
        if (request.type() == LedgerTransactionType.PROVISIONAL_CREDIT) {
            return postProvisionalCredit(customerId, request, correlationId);
        }
        var correlation = parseCorrelation(correlationId);
        var ledger = new LedgerTransaction();
        ledger.setBusinessOperationId(request.businessOperationId());
        ledger.setType(request.type());
        ledger.setStatus(LedgerTransactionStatus.POSTED);
        ledger.setReference(request.reference().trim());
        ledger.setCorrelationId(correlation);
        ledger.setPostedAt(Instant.now());

        var sourceAccount = accountRepository.findWithLockByIdAndCustomerId(request.sourceAccountId(), customerId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
        if (sourceAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountException(HttpStatus.CONFLICT, "ACCOUNT_NOT_ELIGIBLE", "Source account is not eligible for money movement");
        }
        if (!sourceAccount.getCurrency().equals(request.currency())) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Currency must match the source account");
        }
        var sourceBalance = balanceRepository.findWithLockByAccountId(sourceAccount.getId())
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_BALANCE_NOT_FOUND", "Account balance was not found"));
        if (sourceBalance.getAvailableBalance().compareTo(request.amount()) < 0) {
            throw new AccountException(HttpStatus.CONFLICT, "INSUFFICIENT_FUNDS", "Insufficient available balance");
        }

        Account destinationAccount = null;
        AccountBalance destinationBalance = null;
        UUID balancingAccountId = switch (request.type()) {
            case INTERNAL_TRANSFER -> request.destinationAccountId();
            case CREDIT_CARD_PAYMENT -> CREDIT_CARD_CLEARING_ACCOUNT_ID;
            case EXTERNAL_PAYMENT -> EXTERNAL_CLEARING_ACCOUNT_ID;
            case REVERSAL -> request.destinationAccountId();
            case PROVISIONAL_CREDIT -> DISPUTE_CLEARING_ACCOUNT_ID;
        };

        if (request.type() == LedgerTransactionType.INTERNAL_TRANSFER || request.type() == LedgerTransactionType.REVERSAL) {
            destinationAccount = accountRepository.findWithLockByIdAndCustomerId(request.destinationAccountId(), customerId)
                    .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Destination account was not found"));
            if (!destinationAccount.getCurrency().equals(request.currency())) {
                throw new AccountException(HttpStatus.BAD_REQUEST, "CURRENCY_MISMATCH", "Currency must match the destination account");
            }
            destinationBalance = balanceRepository.findWithLockByAccountId(destinationAccount.getId())
                    .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_BALANCE_NOT_FOUND", "Destination balance was not found"));
        }

        sourceBalance.setCurrentBalance(sourceBalance.getCurrentBalance().subtract(request.amount()));
        sourceBalance.setAvailableBalance(sourceBalance.getAvailableBalance().subtract(request.amount()));
        sourceBalance.setAsOf(ledger.getPostedAt());
        balanceRepository.save(sourceBalance);

        if (destinationBalance != null) {
            destinationBalance.setCurrentBalance(destinationBalance.getCurrentBalance().add(request.amount()));
            destinationBalance.setAvailableBalance(destinationBalance.getAvailableBalance().add(request.amount()));
            destinationBalance.setAsOf(ledger.getPostedAt());
            balanceRepository.save(destinationBalance);
        }

        ledger = ledgerTransactionRepository.save(ledger);
        var debit = entry(ledger.getId(), sourceAccount.getId(), LedgerEntryDirection.DEBIT, request.amount(), request.currency(), sourceBalance.getCurrentBalance());
        var credit = entry(ledger.getId(), balancingAccountId, LedgerEntryDirection.CREDIT, request.amount(), request.currency(),
                destinationBalance == null ? BigDecimal.ZERO : destinationBalance.getCurrentBalance());
        ledgerEntryRepository.saveAll(List.of(debit, credit));
        return toResponse(ledger);
    }

    private LedgerTransactionResponse postProvisionalCredit(UUID customerId, PostLedgerTransactionRequest request, String correlationId) {
        var destination = accountRepository.findWithLockByIdAndCustomerId(request.destinationAccountId(), customerId)
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
        if (destination.getStatus() != AccountStatus.ACTIVE || !destination.getCurrency().equals(request.currency())) {
            throw new AccountException(HttpStatus.CONFLICT, "ACCOUNT_NOT_ELIGIBLE", "Account is not eligible for provisional credit");
        }
        var balance = balanceRepository.findWithLockByAccountId(destination.getId())
                .orElseThrow(() -> new AccountException(HttpStatus.NOT_FOUND, "ACCOUNT_BALANCE_NOT_FOUND", "Account balance was not found"));
        var ledger = new LedgerTransaction();
        ledger.setBusinessOperationId(request.businessOperationId());
        ledger.setType(request.type());
        ledger.setStatus(LedgerTransactionStatus.POSTED);
        ledger.setReference(request.reference().trim());
        ledger.setCorrelationId(parseCorrelation(correlationId));
        ledger.setPostedAt(Instant.now());
        balance.setCurrentBalance(balance.getCurrentBalance().add(request.amount()));
        balance.setAvailableBalance(balance.getAvailableBalance().add(request.amount()));
        balance.setAsOf(ledger.getPostedAt());
        balanceRepository.save(balance);
        ledger = ledgerTransactionRepository.save(ledger);
        ledgerEntryRepository.saveAll(List.of(
                entry(ledger.getId(), DISPUTE_CLEARING_ACCOUNT_ID, LedgerEntryDirection.DEBIT, request.amount(), request.currency(), BigDecimal.ZERO),
                entry(ledger.getId(), destination.getId(), LedgerEntryDirection.CREDIT, request.amount(), request.currency(), balance.getCurrentBalance())));
        return toResponse(ledger);
    }

    private LedgerEntry entry(UUID transactionId, UUID accountId, LedgerEntryDirection direction, BigDecimal amount, String currency, BigDecimal balanceAfter) {
        var entry = new LedgerEntry();
        entry.setLedgerTransactionId(transactionId);
        entry.setAccountId(accountId);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setCurrency(currency);
        entry.setBalanceAfter(balanceAfter);
        return entry;
    }

    private void validate(PostLedgerTransactionRequest request) {
        if (request.amount().signum() <= 0) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be positive");
        }
        if ((request.type() == LedgerTransactionType.INTERNAL_TRANSFER || request.type() == LedgerTransactionType.REVERSAL || request.type() == LedgerTransactionType.PROVISIONAL_CREDIT)
                && request.destinationAccountId() == null) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "DESTINATION_REQUIRED", "Destination account is required");
        }
        if (request.sourceAccountId().equals(request.destinationAccountId()) && request.type() != LedgerTransactionType.PROVISIONAL_CREDIT) {
            throw new AccountException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT_TRANSFER", "Source and destination accounts cannot be the same");
        }
    }

    private LedgerTransactionResponse toResponse(LedgerTransaction transaction) {
        var entries = ledgerEntryRepository.findByLedgerTransactionId(transaction.getId()).stream()
                .map(entry -> new LedgerEntryResponse(entry.getAccountId(), entry.getDirection(), entry.getAmount(), entry.getCurrency(), entry.getBalanceAfter(), entry.getCreatedAt()))
                .toList();
        return new LedgerTransactionResponse(
                transaction.getId(),
                transaction.getBusinessOperationId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getReference(),
                transaction.getPostedAt(),
                entries
        );
    }

    private UUID parseCorrelation(String correlationId) {
        try {
            return UUID.fromString(correlationId);
        } catch (RuntimeException ex) {
            return UUID.randomUUID();
        }
    }
}
