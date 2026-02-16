package com.example.demo.meal.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.meal.entity.MealLog;

public interface MealLogRepository extends JpaRepository<MealLog, Long> {

    @Query("SELECT SUM(m.calories) FROM MealLog m WHERE m.date = :date")
    Integer sumCaloriesByDate(LocalDate date);

    @Query("SELECT SUM(m.protein) FROM MealLog m WHERE m.date = :date")
    Integer sumProteinByDate(LocalDate date);

    Long countByDate(LocalDate date);

}
