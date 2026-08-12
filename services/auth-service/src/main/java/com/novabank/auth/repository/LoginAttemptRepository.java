package com.novabank.auth.repository;

import com.novabank.auth.domain.LoginAttempt;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    @Query("select attempt from LoginAttempt attempt where attempt.usernameOrEmail in :identifiers order by attempt.attemptedAt desc")
    List<LoginAttempt> findRecentByUsernameOrEmail(
            @Param("identifiers") List<String> identifiers,
            Pageable pageable);
}
