package com.myfitness.api.training.dto;

import java.util.List;

public record TrainingExerciseResponse(
        Long id,
        String exerciseName,
        String memo,
        List<TrainingSetResponse> sets) {
}
