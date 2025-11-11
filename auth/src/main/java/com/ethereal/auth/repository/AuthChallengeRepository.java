package com.ethereal.auth.repository;

import com.ethereal.auth.entity.AuthChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, UUID> {
    Optional<AuthChallenge> findByIdAndConsumedAtIsNull(UUID id);
}