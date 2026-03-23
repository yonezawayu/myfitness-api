package com.myfitness.api.meal.dto;

import java.math.BigDecimal;

public class MealItemResponse {

    private Long id;
    private Long mealLogId;
    private Long foodId;
    private BigDecimal quantityG;
    private BigDecimal calories;
    private BigDecimal protein;

    public MealItemResponse() {
    }

    public MealItemResponse(
            Long id,
            Long mealLogId,
            Long foodId,
            BigDecimal quantityG,
            BigDecimal calories,
            BigDecimal protein) {
        this.id = id;
        this.mealLogId = mealLogId;
        this.foodId = foodId;
        this.quantityG = quantityG;
        this.calories = calories;
        this.protein = protein;
    }

    public Long getId() {
        return id;
    }

    public Long getMealLogId() {
        return mealLogId;
    }

    public Long getFoodId() {
        return foodId;
    }

    public BigDecimal getQuantityG() {
        return quantityG;
    }

    public BigDecimal getCalories() {
        return calories;
    }

    public BigDecimal getProtein() {
        return protein;
    }
}