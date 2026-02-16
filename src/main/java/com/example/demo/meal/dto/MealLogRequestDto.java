package com.example.demo.meal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class MealLogRequestDto {

    @NotBlank(message = "mealName is required")
    private String mealName;

    @NotNull(message = "calories is required")
    @Min(value = 0, message = "calories must be zero or positive")
    private Integer calories;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "date must be today or past")
    private LocalDate date;

    // optional
    private String memo;

    @Min(value = 0, message = "protein must be zero or positive")
    private Double protein;

    public MealLogRequestDto() {
    }

    public MealLogRequestDto(
            String mealName,
            Integer calories,
            LocalDate date,
            String memo,
            Double protein) {
        this.mealName = mealName;
        this.calories = calories;
        this.date = date;
        this.memo = memo;
        this.protein = protein;

    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }

}
