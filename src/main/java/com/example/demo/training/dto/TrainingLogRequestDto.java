package com.example.demo.training.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class TrainingLogRequestDto {

    @NotBlank(message = "trainingName is required")
    private String trainingName;

    @NotNull(message = "caloriesBurned is required")
    @Min(value = 0, message = "caloriesBurned must be zero or positive")
    private Integer caloriesBurned;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "date must be today or past")
    private LocalDate date;

    public TrainingLogRequestDto() {
    }

    public TrainingLogRequestDto(String trainingName, Integer caloriesBurned, LocalDate date) {
        this.trainingName = trainingName;
        this.caloriesBurned = caloriesBurned;
        this.date = date;
    }

    public String getTrainingName() {
        return trainingName;
    }

    public void setTrainingName(String trainingName) {
        this.trainingName = trainingName;
    }

    public Integer getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(Integer caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
