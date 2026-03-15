package com.myfitness.api.food.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.food.entity.Food;

public interface FoodRepository extends JpaRepository<Food, Long> {
}