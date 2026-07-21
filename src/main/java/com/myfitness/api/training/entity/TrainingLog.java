package com.myfitness.api.training.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class TrainingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String trainingName;

    private LocalDate date;

    public TrainingLog() {
    }

    public TrainingLog(Long id, String trainingName, LocalDate date) {
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
