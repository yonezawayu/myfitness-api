package com.myfitness.api.record.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double weight;
    private int calories;
    private LocalDate date;

    // ← JPA が必須とする「デフォルトコンストラクタ」
    public Record() {
    }

    // ← 必要であれば使う用のコンストラクタ
    public Record(Long id, double weight, int calories, LocalDate date) {
        this.id = id;
        this.weight = weight;
        this.calories = calories;
        this.date = date;
    }

    // getter / setter
    public Long getId() {
        return id;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }
}
