package com.myfitness.api.training.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_sets")
public class TrainingSet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_exercise_id", nullable = false)
    private TrainingExercise trainingExercise;

    @Column(nullable = false)
    private Integer setNumber;

    @Column(precision = 10, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private Integer reps;

    @Column(length = 500)
    private String memo;

    public TrainingSet() {
    }

    public TrainingSet(Integer setNumber, BigDecimal weightKg, Integer reps, String memo) {
        this.setNumber = setNumber;
        this.weightKg = weightKg;
        this.reps = reps;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public TrainingExercise getTrainingExercise() {
        return trainingExercise;
    }

    public void setTrainingExercise(TrainingExercise trainingExercise) {
        this.trainingExercise = trainingExercise;
    }

    public Integer getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(Integer setNumber) {
        this.setNumber = setNumber;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }
}
