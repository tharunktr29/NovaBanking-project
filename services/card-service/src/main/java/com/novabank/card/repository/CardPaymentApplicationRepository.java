package com.novabank.card.repository;

import com.novabank.card.domain.CardPaymentApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardPaymentApplicationRepository extends JpaRepository<CardPaymentApplication, UUID> {
    Optional<CardPaymentApplication> findByBusinessOperationId(UUID businessOperationId);
}
