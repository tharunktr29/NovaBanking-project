package com.novabank.payment.repository;

import com.novabank.payment.domain.ExternalPayee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalPayeeRepository extends JpaRepository<ExternalPayee, UUID> {
    List<ExternalPayee> findByCustomerIdOrderByNicknameAsc(UUID customerId);
    Optional<ExternalPayee> findByIdAndCustomerId(UUID id, UUID customerId);
}
