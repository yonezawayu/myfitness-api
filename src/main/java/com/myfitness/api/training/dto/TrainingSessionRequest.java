package com.myfitness.api.training.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrainingSessionRequest(
        @NotNull LocalDate date,
        @Size(max = 500) String memo) {
}
