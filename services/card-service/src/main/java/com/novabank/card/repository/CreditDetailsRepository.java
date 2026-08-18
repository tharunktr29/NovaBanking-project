package com.novabank.card.repository;

import com.novabank.card.domain.CreditDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditDetailsRepository extends JpaRepository<CreditDetails, UUID> {
    Optional<CreditDetails> findByCardId(UUID cardId);
}
