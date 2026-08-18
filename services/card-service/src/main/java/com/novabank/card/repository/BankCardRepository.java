package com.novabank.card.repository;

import com.novabank.card.domain.BankCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface BankCardRepository extends JpaRepository<BankCard, UUID>, JpaSpecificationExecutor<BankCard> {
    Optional<BankCard> findByIdAndCustomerId(UUID id, UUID customerId);
}
