package com.myfitness.api.training.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.myfitness.api.training.entity.TrainingExercise;

public interface TrainingExerciseRepository extends JpaRepository<TrainingExercise, Long> {

    @Query("""
            SELECT exercise
            FROM TrainingExercise exercise
            WHERE exercise.trainingSession.id = :sessionId
              AND exercise.trainingSession.user.id = :userId
            ORDER BY exercise.id ASC
            """)
    List<TrainingExercise> findAllOwnedBySession(
            @Param("sessionId") Long sessionId,
            @Param("userId") Long userId);

    @Query("""
            SELECT exercise
            FROM TrainingExercise exercise
            WHERE exercise.id = :exerciseId
              AND exercise.trainingSession.id = :sessionId
              AND exercise.trainingSession.user.id = :userId
            """)
    Optional<TrainingExercise> findOwnedById(
            @Param("sessionId") Long sessionId,
            @Param("exerciseId") Long exerciseId,
            @Param("userId") Long userId);
}
