package com.novabank.payment.repository;

import com.novabank.payment.domain.PaymentOrder;
import com.novabank.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, UUID>, JpaSpecificationExecutor<PaymentOrder> {
    Optional<PaymentOrder> findByIdAndCustomerId(UUID id, UUID customerId);
    Page<PaymentOrder> findByCustomerId(UUID customerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentOrder> findWithLockByIdAndCustomerId(UUID id, UUID customerId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentOrder> findWithLockById(UUID id);

    List<PaymentOrder> findTop25ByStatusAndScheduledForLessThanEqualOrderByScheduledForAsc(PaymentStatus status, Instant now);
}
