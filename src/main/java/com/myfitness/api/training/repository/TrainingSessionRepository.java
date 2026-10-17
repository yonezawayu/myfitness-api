package com.myfitness.api.training.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingSession;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    List<TrainingSession> findAllByUserIdOrderByDateDescIdDesc(Long userId);

    Optional<TrainingSession> findByIdAndUserId(Long sessionId, Long userId);
}
