package com.novabank.account.repository;

import com.novabank.account.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    List<Account> findByCustomerIdOrderByAccountTypeAscOpenedDateAsc(UUID customerId);

    Optional<Account> findByIdAndCustomerId(UUID id, UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByIdAndCustomerId(UUID id, UUID customerId);
}
