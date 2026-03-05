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

    private Integer caloriesBurned;

    private LocalDate date;

    public TrainingLog() {
    }

    public TrainingLog(Long id, String trainingName, Integer caloriesBurned, LocalDate date) {
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
