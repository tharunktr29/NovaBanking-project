package com.novabank.transaction.repository;

import com.novabank.transaction.domain.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, UUID> {
    Optional<CustomerAccount> findByAccountIdAndCustomerId(UUID accountId, UUID customerId);
}
