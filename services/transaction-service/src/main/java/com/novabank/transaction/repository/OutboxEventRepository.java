package com.novabank.transaction.repository;

import com.novabank.transaction.domain.OutboxEvent;
import com.novabank.transaction.domain.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(List<OutboxStatus> statuses, Instant nextAttemptAt, Pageable pageable);
}
