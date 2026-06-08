package com.myfitness.api.training.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingExercise;

public interface TrainingExerciseRepository extends JpaRepository<TrainingExercise, Long> {

    List<TrainingExercise> findByTrainingSessionIdOrderByIdAsc(Long trainingSessionId);
}
