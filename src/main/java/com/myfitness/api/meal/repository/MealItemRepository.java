package com.myfitness.api.meal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.meal.entity.MealItem;

public interface MealItemRepository extends JpaRepository<MealItem, Long> {

    List<MealItem> findByMealLogId(Long mealLogId);
}