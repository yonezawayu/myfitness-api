package com.myfitness.api.food.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateFoodRequest {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal caloriesPer100g;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal proteinPer100g;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal fatPer100g;

    @NotNull
    @DecimalMin(value = "0.0")
    private BigDecimal carbPer100g;
}