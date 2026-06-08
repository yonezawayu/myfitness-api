package com.myfitness.api.training.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "training_exercises")
public class TrainingExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "training_session_id", nullable = false)
    private TrainingSession trainingSession;

    @Column(nullable = false)
    private String exerciseName;

    @Column(length = 500)
    private String memo;

    @OneToMany(mappedBy = "trainingExercise", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrainingSet> sets = new ArrayList<>();

    public TrainingExercise() {
    }

    public TrainingExercise(String exerciseName, String memo) {
        this.exerciseName = exerciseName;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public TrainingSession getTrainingSession() {
        return trainingSession;
    }

    public void setTrainingSession(TrainingSession trainingSession) {
        this.trainingSession = trainingSession;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public List<TrainingSet> getSets() {
        return sets;
    }

    public void addSet(TrainingSet trainingSet) {
        sets.add(trainingSet);
        trainingSet.setTrainingExercise(this);
    }
}
