package com.myfitness.api.meal.dto;

import java.time.LocalDate;

public class MealLogResponseDto {

    private Long id;
    private String mealName;
    private Integer calories;
    private LocalDate date;
    private String memo;
    private Double protein;

    public MealLogResponseDto() {
    }

    public MealLogResponseDto(Long id, String mealName, Integer calories,
            LocalDate date, String memo, Double protein) {
        this.id = id;
        this.mealName = mealName;
        this.calories = calories;
        this.date = date;
        this.memo = memo;
        this.protein = protein;
    }

    public Long getId() {
        return id;
    }

    public String getMealName() {
        return mealName;
    }

    public Integer getCalories() {
        return calories;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMemo() {
        return memo;
    }

    public Double getProtein() {
        return protein;
    }
}
