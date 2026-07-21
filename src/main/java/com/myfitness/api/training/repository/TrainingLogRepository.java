package com.myfitness.api.training.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingLog;

public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {
}
