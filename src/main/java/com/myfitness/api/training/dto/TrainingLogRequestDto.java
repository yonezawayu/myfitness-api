package com.myfitness.api.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class TrainingLogRequestDto {

    @NotBlank(message = "trainingName is required")
    private String trainingName;

    @NotNull(message = "date is required")
    @PastOrPresent(message = "date must be today or past")
    private LocalDate date;

    public TrainingLogRequestDto() {
    }

    public TrainingLogRequestDto(String trainingName, LocalDate date) {
        this.trainingName = trainingName;
        this.date = date;
    }

    public String getTrainingName() {
        return trainingName;
    }

    public void setTrainingName(String trainingName) {
        this.trainingName = trainingName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
