package com.myfitness.api.training.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingSessionAggregateRequest(
        @NotNull LocalDate date,
        @Size(max = 500) String memo,
        @NotEmpty @Size(max = 50) List<@Valid Exercise> exercises) {

    public record Exercise(
            @NotBlank @Size(max = 255) String exerciseName,
            @Size(max = 500) String memo,
            @NotEmpty @Size(max = 100) List<@Valid Set> sets) {
    }

    public record Set(
            @NotNull @Min(1) Integer setNumber,
            @DecimalMin("0.0") BigDecimal weightKg,
            @NotNull @Min(1) Integer reps,
            @Size(max = 500) String memo) {
    }
}
