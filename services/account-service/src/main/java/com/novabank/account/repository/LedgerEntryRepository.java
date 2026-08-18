package com.novabank.account.repository;

import com.novabank.account.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByLedgerTransactionId(UUID ledgerTransactionId);
}
