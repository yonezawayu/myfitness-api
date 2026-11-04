package com.myfitness.api.training.service;

import com.myfitness.api.training.dto.TrainingSessionResponse;

public record TrainingSessionCreationResult(
        TrainingSessionResponse response,
        boolean replayed) {
}
