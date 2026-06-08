package com.myfitness.api.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrainingExerciseRequest(
        @NotBlank @Size(max = 255) String exerciseName,
        @Size(max = 500) String memo) {
}
