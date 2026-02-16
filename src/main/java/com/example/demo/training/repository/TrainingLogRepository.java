package com.example.demo.training.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.training.entity.TrainingLog;

public interface TrainingLogRepository extends JpaRepository<TrainingLog, Long> {

    @Query("SELECT SUM(t.caloriesBurned) FROM TrainingLog t WHERE t.date = :date")
    Integer sumCaloriesByDate(LocalDate date);
}
