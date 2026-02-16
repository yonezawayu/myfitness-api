package com.example.demo.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDate;

public class RecordRequestDto {

    @NotNull(message = "weight is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "weight must be positive")
    private Double weight;

    @NotNull(message = "calories is required")
    @Min(value = 0, message = "calories must be zero or positive")
    private Integer calories;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "date must be today or past")
    private LocalDate date;

    public RecordRequestDto() {
    }

    public RecordRequestDto(Double weight, Integer calories, LocalDate date) {
        this.weight = weight;
        this.calories = calories;
        this.date = date;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
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
}
