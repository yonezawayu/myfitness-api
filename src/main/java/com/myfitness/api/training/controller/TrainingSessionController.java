package com.myfitness.api.training.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public List<TrainingSessionResponse> findAllSessions(Authentication authentication) {
        return trainingSessionService.findAllSessions(authentication.getName());
    }

    @GetMapping("/{sessionId}")
    public TrainingSessionResponse findSessionById(
            Authentication authentication,
            @PathVariable Long sessionId) {
        return trainingSessionService.findSessionById(authentication.getName(), sessionId);
    }

    @PostMapping
    public ResponseEntity<TrainingSessionResponse> createSession(
            Authentication authentication,
            @Valid @RequestBody TrainingSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createSession(authentication.getName(), request));
    }

    @PutMapping("/{sessionId}")
    public TrainingSessionResponse updateSession(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody TrainingSessionRequest request) {
        return trainingSessionService.updateSession(authentication.getName(), sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            Authentication authentication,
            @PathVariable Long sessionId) {
        trainingSessionService.deleteSession(authentication.getName(), sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/exercises")
    public List<TrainingExerciseResponse> findAllExercises(
            Authentication authentication,
            @PathVariable Long sessionId) {
        return trainingSessionService.findAllExercises(authentication.getName(), sessionId);
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}")
    public TrainingExerciseResponse findExerciseById(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        return trainingSessionService.findExerciseById(authentication.getName(), sessionId, exerciseId);
    }

    @PostMapping("/{sessionId}/exercises")
    public ResponseEntity<TrainingExerciseResponse> createExercise(
            Authentication authentication,
            @PathVariable Long sessionId,
            @Valid @RequestBody TrainingExerciseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createExercise(authentication.getName(), sessionId, request));
    }

    @PutMapping("/{sessionId}/exercises/{exerciseId}")
    public TrainingExerciseResponse updateExercise(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody TrainingExerciseRequest request) {
        return trainingSessionService.updateExercise(
                authentication.getName(),
                sessionId,
                exerciseId,
                request);
    }

    @DeleteMapping("/{sessionId}/exercises/{exerciseId}")
    public ResponseEntity<Void> deleteExercise(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        trainingSessionService.deleteExercise(authentication.getName(), sessionId, exerciseId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}/sets")
    public List<TrainingSetResponse> findAllSets(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId) {
        return trainingSessionService.findAllSets(authentication.getName(), sessionId, exerciseId);
    }

    @GetMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public TrainingSetResponse findSetById(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId) {
        return trainingSessionService.findSetById(
                authentication.getName(),
                sessionId,
                exerciseId,
                setId);
    }

    @PostMapping("/{sessionId}/exercises/{exerciseId}/sets")
    public ResponseEntity<TrainingSetResponse> createSet(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @Valid @RequestBody TrainingSetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(trainingSessionService.createSet(
                        authentication.getName(),
                        sessionId,
                        exerciseId,
                        request));
    }

    @PutMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public TrainingSetResponse updateSet(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId,
            @Valid @RequestBody TrainingSetRequest request) {
        return trainingSessionService.updateSet(
                authentication.getName(),
                sessionId,
                exerciseId,
                setId,
                request);
    }

    @DeleteMapping("/{sessionId}/exercises/{exerciseId}/sets/{setId}")
    public ResponseEntity<Void> deleteSet(
            Authentication authentication,
            @PathVariable Long sessionId,
            @PathVariable Long exerciseId,
            @PathVariable Long setId) {
        trainingSessionService.deleteSet(authentication.getName(), sessionId, exerciseId, setId);
        return ResponseEntity.noContent().build();
    }
}
