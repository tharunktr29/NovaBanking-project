package com.novabank.payment.repository;

import com.novabank.payment.domain.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {
    List<PaymentStatusHistory> findByPaymentIdOrderByOccurredAtAsc(UUID paymentId);
}
