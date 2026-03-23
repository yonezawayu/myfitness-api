package com.myfitness.api.meal.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "meal_items")
public class MealItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meal_log_id", nullable = false)
    private Long mealLogId;

    @Column(name = "food_id", nullable = false)
    private Long foodId;

    @Column(name = "quantity_g", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantityG;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal calories;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal protein;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MealItem() {
    }

    public MealItem(
            Long mealLogId,
            Long foodId,
            BigDecimal quantityG,
            BigDecimal calories,
            BigDecimal protein) {
        this.mealLogId = mealLogId;
        this.foodId = foodId;
        this.quantityG = quantityG;
        this.calories = calories;
        this.protein = protein;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getMealLogId() {
        return mealLogId;
    }

    public Long getFoodId() {
        return foodId;
    }

    public BigDecimal getQuantityG() {
        return quantityG;
    }

    public BigDecimal getCalories() {
        return calories;
    }

    public BigDecimal getProtein() {
        return protein;
    }
}