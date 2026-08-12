package com.novabank.auth.repository;

import com.novabank.auth.domain.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {
    Optional<UserCredential> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);
}
