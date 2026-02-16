package com.example.demo.meal.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class MealLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 食事名（必須）
    @Column(nullable = false)
    private String mealName;

    // カロリー（必須）
    @Column(nullable = false)
    private Integer calories;

    // 日付（必須）
    @Column(nullable = false)
    private LocalDate date;

    // メモ（任意）
    private String memo;

    // タンパク質量（任意）
    private Double protein;

    // JPA 必須のデフォルトコンストラクタ
    public MealLog() {
    }

    public MealLog(Long id, String mealName, Integer calories, LocalDate date, String memo, Double protein) {
        this.id = id;
        this.mealName = mealName;
        this.calories = calories;
        this.date = date;
        this.memo = memo;
        this.protein = protein;
    }

    public Long getId() {
        return id;
    }

    public String getMealName() {
        return mealName;
    }

    public void setMealName(String mealName) {
        this.mealName = mealName;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public Double getProtein() {
        return protein;
    }

    public void setProtein(Double protein) {
        this.protein = protein;
    }
}
