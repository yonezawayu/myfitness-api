package com.myfitness.api.training.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingSession;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {

    List<TrainingSession> findAllByOrderByDateDescIdDesc();
}
