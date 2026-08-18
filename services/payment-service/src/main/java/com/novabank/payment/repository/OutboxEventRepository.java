package com.novabank.payment.repository;

import com.novabank.payment.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE status IN (:statuses)
              AND next_attempt_at <= :now
            ORDER BY created_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findDueForUpdate(@Param("statuses") List<String> statuses, @Param("now") Instant now, @Param("limit") int limit);
}
