package com.novabank.card.repository;

import com.novabank.card.domain.CardLifecycleHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CardLifecycleHistoryRepository extends JpaRepository<CardLifecycleHistory, UUID> {
    List<CardLifecycleHistory> findByCardIdOrderByOccurredAtDesc(UUID cardId);
}
