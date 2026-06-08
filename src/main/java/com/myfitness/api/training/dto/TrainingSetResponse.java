package com.myfitness.api.training.dto;

import java.math.BigDecimal;

public record TrainingSetResponse(
        Long id,
        Integer setNumber,
        BigDecimal weightKg,
        Integer reps,
        String memo) {
}
