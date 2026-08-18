package com.novabank.card.repository;

import com.novabank.card.domain.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {
    Optional<IdempotencyRecord> findByCustomerIdAndOperationAndResourceIdAndIdempotencyKey(
            UUID customerId,
            String operation,
            UUID resourceId,
            String idempotencyKey
    );
}
