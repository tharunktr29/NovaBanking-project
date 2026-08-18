package com.novabank.account.repository;

import com.novabank.account.domain.LedgerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {
    Optional<LedgerTransaction> findByBusinessOperationId(UUID businessOperationId);
}
