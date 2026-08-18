package com.novabank.card.repository;

import com.novabank.card.domain.CardControls;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CardControlsRepository extends JpaRepository<CardControls, UUID> {
    Optional<CardControls> findByCardId(UUID cardId);
}
