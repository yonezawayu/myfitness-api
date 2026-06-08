package com.myfitness.api.training.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingSetRequest(
        @NotNull @Min(1) Integer setNumber,
        @DecimalMin("0.0") BigDecimal weightKg,
        @NotNull @Min(1) Integer reps,
        @Size(max = 500) String memo) {
}
