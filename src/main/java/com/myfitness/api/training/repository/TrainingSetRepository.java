package com.myfitness.api.training.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.training.entity.TrainingSet;

public interface TrainingSetRepository extends JpaRepository<TrainingSet, Long> {

    List<TrainingSet> findByTrainingExerciseIdOrderBySetNumberAscIdAsc(Long trainingExerciseId);
}
