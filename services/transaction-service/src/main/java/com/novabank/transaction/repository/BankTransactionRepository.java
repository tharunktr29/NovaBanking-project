package com.novabank.transaction.repository;

import com.novabank.transaction.domain.BankTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, UUID>, JpaSpecificationExecutor<BankTransaction> {
    Optional<BankTransaction> findBySourceReference(String sourceReference);

    Optional<BankTransaction> findByIdAndCustomerId(UUID id, UUID customerId);
}
