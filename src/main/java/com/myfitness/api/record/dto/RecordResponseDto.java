package com.myfitness.api.record.dto;

import java.time.LocalDate;

public class RecordResponseDto {

    private Long id;
    private double weight;
    private int calories;
    private LocalDate date;

    public RecordResponseDto() {
    }

    public RecordResponseDto(Long id, double weight, int calories, LocalDate date) {
        this.id = id;
        this.weight = weight;
        this.calories = calories;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public double getWeight() {
        return weight;
    }

    public int getCalories() {
        return calories;
    }

    public LocalDate getDate() {
        return date;
    }
}
