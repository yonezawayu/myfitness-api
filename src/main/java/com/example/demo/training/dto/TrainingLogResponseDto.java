package com.example.demo.training.dto;

import java.time.LocalDate;

public class TrainingLogResponseDto {

    private Long id;
    private String trainingName;
    private Integer caloriesBurned;
    private LocalDate date;

    public TrainingLogResponseDto() {
    }

    public TrainingLogResponseDto(
            Long id,
            String trainingName,
            Integer caloriesBurned,
            LocalDate date) {

        this.id = id;
        this.trainingName = trainingName;
        this.caloriesBurned = caloriesBurned;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public String getTrainingName() {
        return trainingName;
    }

    public Integer getCaloriesBurned() {
        return caloriesBurned;
    }

    public LocalDate getDate() {
        return date;
    }
}
