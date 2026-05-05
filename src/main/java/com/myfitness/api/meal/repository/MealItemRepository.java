package com.myfitness.api.meal.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.myfitness.api.meal.entity.MealItem;

public interface MealItemRepository extends JpaRepository<MealItem, Long> {

    List<MealItem> findByMealLogId(Long mealLogId);

    @Query("SELECT COALESCE(SUM(m.calories), 0) FROM MealItem m WHERE m.mealLogId = :mealLogId")
    BigDecimal sumCaloriesByMealLogId(Long mealLogId);

    @Query("SELECT COALESCE(SUM(m.protein), 0) FROM MealItem m WHERE m.mealLogId = :mealLogId")
    BigDecimal sumProteinByMealLogId(Long mealLogId);
}