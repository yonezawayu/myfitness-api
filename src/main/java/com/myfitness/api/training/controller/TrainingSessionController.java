package com.myfitness.api.training.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.myfitness.api.training.dto.TrainingExerciseRequest;
import com.myfitness.api.training.dto.TrainingExerciseResponse;
import com.myfitness.api.training.dto.TrainingSessionRequest;
import com.myfitness.api.training.dto.TrainingSessionResponse;
import com.myfitness.api.training.dto.TrainingSetRequest;
import com.myfitness.api.training.dto.TrainingSetResponse;
import com.myfitness.api.training.service.TrainingSessionService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Training Session", description = "Training session, exercise, and set CRUD APIs")
@RestController
@RequestMapping("/training-sessions")
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @GetMapping
    public List<TrainingSessionResponse> findAllSessions() {
        return trainingSessionService.findAllSessions();
    }

    @GetMapping("/{sessionId}")
    public TrainingSessionResponse findSessionById(@PathVariable Long sessionId) {
        return trainingSessionService.findSessionById(sessionId);
    }

    @PostMapping
    public ResponseEntity<TrainingSessionResponse> createSession(
            @Valid @RequestBody TrainingSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createSession(request));
    }

    @PutMapping("/{sessionId}")
    public TrainingSessionResponse updateSession(
            @PathVariable Long sessionId,
            @Valid @RequestBody TrainingSessionRequest request) {
        return trainingSessionService.updateSession(sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long sessionId) {
        trainingSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/exercises")
    public List<TrainingExerciseResponse> findAllExercises(@PathVariable Long sessionId) {
        return trainingSessionService.findAllExercises(sessionId);
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}")
    public TrainingExerciseResponse findExerciseById(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        return trainingSessionService.findExerciseById(sessionId, exerciseId);
    }

    @PostMapping("/{sessionId}/exercises")
    public ResponseEntity<TrainingExerciseResponse> createExercise(
            @PathVariable Long sessionId,
            @Valid @RequestBody TrainingExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createExercise(sessionId, request));
    }

    @PutMapping("/{sessionId}/exercises/{exerciseId}")
    public TrainingExerciseResponse updateExercise(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody TrainingExerciseRequest request) {
        return trainingSessionService.updateExercise(sessionId, exerciseId, request);
    }

    @DeleteMapping("/{sessionId}/exercises/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        trainingSessionService.deleteExercise(sessionId, exerciseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}/sets")
    public List<TrainingSetResponse> findAllSets(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        return trainingSessionService.findAllSets(sessionId, exerciseId);
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public TrainingSetResponse findSetById(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId) {
        return trainingSessionService.findSetById(sessionId, exerciseId, setId);
    }

    @PostMapping("/{sessionId}/exercises/{exerciseId}/sets")
    public ResponseEntity<TrainingSetResponse> createSet(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody TrainingSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createSet(sessionId, exerciseId, request));
    }

    @PutMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public TrainingSetResponse updateSet(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId,
            @Valid @RequestBody TrainingSetRequest request) {
        return trainingSessionService.updateSet(sessionId, exerciseId, setId, request);
    }

    @DeleteMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<Void> deleteSet(
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId) {
        trainingSessionService.deleteSet(sessionId, exerciseId, setId);
        return ResponseEntity.noContent().build();
    }
}
