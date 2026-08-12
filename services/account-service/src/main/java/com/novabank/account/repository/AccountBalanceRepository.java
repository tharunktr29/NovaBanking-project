package com.novabank.account.repository;

import com.novabank.account.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    Optional<AccountBalance> findByAccountId(UUID accountId);
}
