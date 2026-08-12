package com.novabank.customer.repository;

import com.novabank.customer.domain.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, UUID> {
    Optional<CustomerProfile> findByAuthUserId(UUID authUserId);
}
