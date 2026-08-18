package com.novabank.card.repository;

import com.novabank.card.domain.ReplacementRequest;
import com.novabank.card.domain.ReplacementRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReplacementRequestRepository extends JpaRepository<ReplacementRequest, UUID> {
    boolean existsByCardIdAndStatusIn(UUID cardId, Collection<ReplacementRequestStatus> statuses);
    Optional<ReplacementRequest> findTopByCardIdOrderByRequestedAtDesc(UUID cardId);
}
