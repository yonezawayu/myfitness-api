package com.myfitness.api.dashboard.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

public class DashboardResponseDto {

    private LocalDate date;
    @Schema(description = "本日の総摂取カロリー（未登録の場合0）", example = "1500")
    private int totalCalories;
    private int totalProtein;
    private long mealCount;
    @Schema(description = "本日の体重（未登録の場合null）", example = "70.5", nullable = true)
    private Double todayWeight;
    @Schema(description = "前日との差分（比較不可の場合null）", example = "-0.3", nullable = true)
    private Double weightDiffFromYesterday;
    private int totalTrainingCalories;

    public DashboardResponseDto(
            LocalDate date,
            int totalCalories,
            int totalProtein,
            long mealCount,
            Double todayWeight,
            Double weightDiffFromYesterday,
            int totalTrainingCalories) {
        this.date = date;
        this.totalCalories = totalCalories;
        this.totalProtein = totalProtein;
        this.mealCount = mealCount;
        this.todayWeight = todayWeight;
        this.weightDiffFromYesterday = weightDiffFromYesterday;
        this.totalTrainingCalories = totalTrainingCalories;
    }

    // Getter
    public LocalDate getDate() {
        return date;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public int getTotalProtein() {
        return totalProtein;
    }

    public long getMealCount() {
        return mealCount;
    }

    public Double getTodayWeight() {
        return todayWeight;
    }

    public Double getWeightDiffFromYesterday() {
        return weightDiffFromYesterday;
    }

    public Integer getTotalTrainingCalories() {
        return totalTrainingCalories;
    }
}
