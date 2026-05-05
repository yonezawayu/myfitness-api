package com.myfitness.api.meal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MealLogResponseDto {

    private Long id;
    private String mealName;
    private LocalDate date;
    private String memo;

    private BigDecimal totalCalories;
    private BigDecimal totalProtein;

    public MealLogResponseDto() {
    }

    public MealLogResponseDto(Long id, String mealName,
            LocalDate date, String memo,
            BigDecimal totalCalories, BigDecimal totalProtein) {

        this.id = id;
        this.mealName = mealName;
        this.date = date;
        this.memo = memo;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
    }

    public Long getId() {
        return id;
    }

    public String getMealName() {
        return mealName;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getMemo() {
        return memo;
    }

    public BigDecimal getTotalCalories() {
        return totalCalories;
    }

    public BigDecimal getTotalProtein() {
        return totalProtein;
    }
}