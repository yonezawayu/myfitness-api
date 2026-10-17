package com.myfitness.api.training.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myfitness.api.training.entity.TrainingSet;

public interface TrainingSetRepository extends JpaRepository<TrainingSet, Long> {

    @Query("""
            SELECT trainingSet
            FROM TrainingSet trainingSet
            WHERE trainingSet.trainingExercise.id = :exerciseId
              AND trainingSet.trainingExercise.trainingSession.id = :sessionId
              AND trainingSet.trainingExercise.trainingSession.user.id = :userId
            ORDER BY trainingSet.setNumber ASC, trainingSet.id ASC
            """)
    List<TrainingSet> findAllOwnedByExercise(
            @Param("sessionId") Long sessionId,
            @Param("exerciseId") Long exerciseId,
            @Param("userId") Long userId);

    @Query("""
            SELECT trainingSet
            FROM TrainingSet trainingSet
            WHERE trainingSet.id = :setId
              AND trainingSet.trainingExercise.id = :exerciseId
              AND trainingSet.trainingExercise.trainingSession.id = :sessionId
              AND trainingSet.trainingExercise.trainingSession.user.id = :userId
            """)
    Optional<TrainingSet> findOwnedById(
            @Param("sessionId") Long sessionId,
            @Param("exerciseId") Long exerciseId,
            @Param("setId") Long setId,
            @Param("userId") Long userId);
}
