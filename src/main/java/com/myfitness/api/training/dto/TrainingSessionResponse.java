package com.myfitness.api.training.dto;

import java.time.LocalDate;
import java.util.List;

public record TrainingSessionResponse(
        Long id,
        LocalDate date,
        String memo,
        List<TrainingExerciseResponse> exercises) {
}
