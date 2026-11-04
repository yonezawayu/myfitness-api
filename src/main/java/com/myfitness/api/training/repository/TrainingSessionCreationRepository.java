package com.myfitness.api.training.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingSessionCreation;

public interface TrainingSessionCreationRepository extends JpaRepository<TrainingSessionCreation, Long> {

    Optional<TrainingSessionCreation> findByUserIdAndIdempotencyKey(Long userId, UUID idempotencyKey);
}
