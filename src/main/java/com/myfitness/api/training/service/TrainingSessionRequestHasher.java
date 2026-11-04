package com.myfitness.api.training.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.myfitness.api.training.dto.TrainingSessionAggregateRequest;

final class TrainingSessionRequestHasher {

    private TrainingSessionRequestHasher() {
    }

    static String hash(TrainingSessionAggregateRequest request) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, request.date().toString());
        append(canonical, request.memo());
        canonical.append(request.exercises().size()).append(':');

        for (TrainingSessionAggregateRequest.Exercise exercise : request.exercises()) {
            append(canonical, exercise.exerciseName());
            append(canonical, exercise.memo());
            canonical.append(exercise.sets().size()).append(':');

            for (TrainingSessionAggregateRequest.Set trainingSet : exercise.sets()) {
                append(canonical, trainingSet.setNumber().toString());
                append(canonical, normalize(trainingSet.weightKg()));
                append(canonical, trainingSet.reps().toString());
                append(canonical, trainingSet.memo());
            }
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static String normalize(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private static void append(StringBuilder target, String value) {
        if (value == null) {
            target.append("-1:");
            return;
        }
        target.append(value.length()).append(':').append(value);
    }
}
