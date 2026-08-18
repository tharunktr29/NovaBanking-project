package com.novabank.account.repository;

import com.novabank.account.domain.AccountBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface AccountBalanceRepository extends JpaRepository<AccountBalance, UUID> {
    Optional<AccountBalance> findByAccountId(UUID accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountBalance> findWithLockByAccountId(UUID accountId);
}
