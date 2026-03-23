package com.myfitness.api.meal.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public class CreateMealItemRequest {

    @NotNull(message = "mealLogId is required")
    private Long mealLogId;

    @NotNull(message = "foodId is required")
    private Long foodId;

    @NotNull(message = "quantityG is required")
    @DecimalMin(value = "0.01", message = "quantityG must be greater than 0")
    private BigDecimal quantityG;

    public CreateMealItemRequest() {
    }

    public CreateMealItemRequest(Long mealLogId, Long foodId, BigDecimal quantityG) {
        this.mealLogId = mealLogId;
        this.foodId = foodId;
        this.quantityG = quantityG;
    }

    public Long getMealLogId() {
        return mealLogId;
    }

    public void setMealLogId(Long mealLogId) {
        this.mealLogId = mealLogId;
    }

    public Long getFoodId() {
        return foodId;
    }

    public void setFoodId(Long foodId) {
        this.foodId = foodId;
    }

    public BigDecimal getQuantityG() {
        return quantityG;
    }

    public void setQuantityG(BigDecimal quantityG) {
        this.quantityG = quantityG;
    }
}