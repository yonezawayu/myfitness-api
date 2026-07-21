package com.myfitness.api.training.dto;

import java.time.LocalDate;

public class TrainingLogResponseDto {

    private Long id;
    private String trainingName;
    private LocalDate date;

    public TrainingLogResponseDto() {
    }

    public TrainingLogResponseDto(
            Long id,
            String trainingName,
            LocalDate date) {

        this.id = id;
        this.trainingName = trainingName;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public String getTrainingName() {
        return trainingName;
    }

    public LocalDate getDate() {
        return date;
    }
}
