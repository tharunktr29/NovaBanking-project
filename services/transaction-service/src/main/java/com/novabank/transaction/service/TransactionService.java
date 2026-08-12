package com.novabank.transaction.service;

import com.novabank.transaction.dto.PageResponse;
import com.novabank.transaction.dto.TransactionQuery;
import com.novabank.transaction.dto.TransactionResponse;
import com.novabank.transaction.exception.TransactionException;
import com.novabank.transaction.repository.BankTransactionRepository;
import com.novabank.transaction.repository.CustomerAccountRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TransactionService {
    private final BankTransactionRepository transactionRepository;
    private final CustomerAccountRepository accountRepository;
    private final TransactionMapper mapper;
    private final TransactionQueryParser queryParser;

    public TransactionService(
            BankTransactionRepository transactionRepository,
            CustomerAccountRepository accountRepository,
            TransactionMapper mapper,
            TransactionQueryParser queryParser
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.mapper = mapper;
        this.queryParser = queryParser;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> list(UUID customerId, TransactionQuery query) {
        validateOwnedAccountFilter(customerId, query.accountId());
        var page = transactionRepository.findAll(
                TransactionSpecifications.byCustomerAndQuery(customerId, query),
                PageRequest.of(query.page(), query.size(), sort(query.sort()))
        );
        return new PageResponse<>(
                page.getContent().stream().map(mapper::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                query.sort()
        );
    }

    @Transactional(readOnly = true)
    public TransactionResponse detail(UUID customerId, UUID transactionId) {
        return transactionRepository.findByIdAndCustomerId(transactionId, customerId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new TransactionException(HttpStatus.NOT_FOUND, "TRANSACTION_NOT_FOUND", "Transaction was not found"));
    }

    public Sort sort(String sort) {
        var parts = sort.split(",");
        var property = queryParser.toJpaSortProperty(parts[0]);
        var direction = parts[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, property);
    }

    public void validateOwnedAccountFilter(UUID customerId, UUID accountId) {
        if (accountId == null) {
            return;
        }
        accountRepository.findByAccountIdAndCustomerId(accountId, customerId)
                .orElseThrow(() -> new TransactionException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Account was not found"));
    }
}
