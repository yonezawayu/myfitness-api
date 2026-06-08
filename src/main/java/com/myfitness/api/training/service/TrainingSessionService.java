package com.myfitness.api.training.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.training.dto.TrainingExerciseRequest;
import com.myfitness.api.training.dto.TrainingExerciseResponse;
import com.myfitness.api.training.dto.TrainingSessionRequest;
import com.myfitness.api.training.dto.TrainingSessionResponse;
import com.myfitness.api.training.dto.TrainingSetRequest;
import com.myfitness.api.training.dto.TrainingSetResponse;
import com.myfitness.api.training.entity.TrainingExercise;
import com.myfitness.api.training.entity.TrainingSession;
import com.myfitness.api.training.entity.TrainingSet;
import com.myfitness.api.training.repository.TrainingExerciseRepository;
import com.myfitness.api.training.repository.TrainingSessionRepository;
import com.myfitness.api.training.repository.TrainingSetRepository;

@Service
@Transactional(readOnly = true)
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingExerciseRepository trainingExerciseRepository;
    private final TrainingSetRepository trainingSetRepository;

    public TrainingSessionService(
            TrainingSessionRepository trainingSessionRepository,
            TrainingExerciseRepository trainingExerciseRepository,
            TrainingSetRepository trainingSetRepository) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingExerciseRepository = trainingExerciseRepository;
        this.trainingSetRepository = trainingSetRepository;
    }

    public List<TrainingSessionResponse> findAllSessions() {
        return trainingSessionRepository.findAllByOrderByDateDescIdDesc().stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public TrainingSessionResponse findSessionById(Long sessionId) {
        return toSessionResponse(findSession(sessionId));
    }

    @Transactional
    public TrainingSessionResponse createSession(TrainingSessionRequest request) {
        TrainingSession trainingSession = new TrainingSession(request.date(), request.memo());
        return toSessionResponse(trainingSessionRepository.save(trainingSession));
    }

    @Transactional
    public TrainingSessionResponse updateSession(Long sessionId, TrainingSessionRequest request) {
        TrainingSession trainingSession = findSession(sessionId);
        trainingSession.setDate(request.date());
        trainingSession.setMemo(request.memo());
        return toSessionResponse(trainingSession);
    }

    @Transactional
    public void deleteSession(Long sessionId) {
        trainingSessionRepository.delete(findSession(sessionId));
    }

    public List<TrainingExerciseResponse> findAllExercises(Long sessionId) {
        findSession(sessionId);
        return trainingExerciseRepository.findByTrainingSessionIdOrderByIdAsc(sessionId).stream()
                .map(this::toExerciseResponse)
                .toList();
    }

    public TrainingExerciseResponse findExerciseById(Long sessionId, Long exerciseId) {
        return toExerciseResponse(findExercise(sessionId, exerciseId));
    }

    @Transactional
    public TrainingExerciseResponse createExercise(Long sessionId, TrainingExerciseRequest request) {
        TrainingSession trainingSession = findSession(sessionId);
        TrainingExercise trainingExercise = new TrainingExercise(request.exerciseName(), request.memo());
        trainingSession.addExercise(trainingExercise);
        return toExerciseResponse(trainingExerciseRepository.save(trainingExercise));
    }

    @Transactional
    public TrainingExerciseResponse updateExercise(
            Long sessionId,
            Long exerciseId,
            TrainingExerciseRequest request) {
        TrainingExercise trainingExercise = findExercise(sessionId, exerciseId);
        trainingExercise.setExerciseName(request.exerciseName());
        trainingExercise.setMemo(request.memo());
        return toExerciseResponse(trainingExercise);
    }

    @Transactional
    public void deleteExercise(Long sessionId, Long exerciseId) {
        trainingExerciseRepository.delete(findExercise(sessionId, exerciseId));
    }

    public List<TrainingSetResponse> findAllSets(Long sessionId, Long exerciseId) {
        findExercise(sessionId, exerciseId);
        return trainingSetRepository.findByTrainingExerciseIdOrderBySetNumberAscIdAsc(exerciseId).stream()
                .map(this::toSetResponse)
                .toList();
    }

    public TrainingSetResponse findSetById(Long sessionId, Long exerciseId, Long setId) {
        return toSetResponse(findSet(sessionId, exerciseId, setId));
    }

    @Transactional
    public TrainingSetResponse createSet(Long sessionId, Long exerciseId, TrainingSetRequest request) {
        TrainingExercise trainingExercise = findExercise(sessionId, exerciseId);
        TrainingSet trainingSet = new TrainingSet(
                request.setNumber(),
                request.weightKg(),
                request.reps(),
                request.memo());
        trainingExercise.addSet(trainingSet);
        return toSetResponse(trainingSetRepository.save(trainingSet));
    }

    @Transactional
    public TrainingSetResponse updateSet(
            Long sessionId,
            Long exerciseId,
            Long setId,
            TrainingSetRequest request) {
        TrainingSet trainingSet = findSet(sessionId, exerciseId, setId);
        trainingSet.setSetNumber(request.setNumber());
        trainingSet.setWeightKg(request.weightKg());
        trainingSet.setReps(request.reps());
        trainingSet.setMemo(request.memo());
        return toSetResponse(trainingSet);
    }

    @Transactional
    public void deleteSet(Long sessionId, Long exerciseId, Long setId) {
        trainingSetRepository.delete(findSet(sessionId, exerciseId, setId));
    }

    private TrainingSession findSession(Long sessionId) {
        return trainingSessionRepository.findById(sessionId)
                .orElseThrow(() -> notFound("TrainingSession", sessionId));
    }

    private TrainingExercise findExercise(Long sessionId, Long exerciseId) {
        TrainingExercise trainingExercise = trainingExerciseRepository.findById(exerciseId)
                .orElseThrow(() -> notFound("TrainingExercise", exerciseId));

        if (!trainingExercise.getTrainingSession().getId().equals(sessionId)) {
            throw notFound("TrainingExercise", exerciseId);
        }

        return trainingExercise;
    }

    private TrainingSet findSet(Long sessionId, Long exerciseId, Long setId) {
        findExercise(sessionId, exerciseId);
        TrainingSet trainingSet = trainingSetRepository.findById(setId)
                .orElseThrow(() -> notFound("TrainingSet", setId));

        if (!trainingSet.getTrainingExercise().getId().equals(exerciseId)) {
            throw notFound("TrainingSet", setId);
        }

        return trainingSet;
    }

    private TrainingSessionResponse toSessionResponse(TrainingSession trainingSession) {
        List<TrainingExerciseResponse> exercises = trainingExerciseRepository
                .findByTrainingSessionIdOrderByIdAsc(trainingSession.getId()).stream()
                .map(this::toExerciseResponse)
                .toList();

        return new TrainingSessionResponse(
                trainingSession.getId(),
                trainingSession.getDate(),
                trainingSession.getMemo(),
                exercises);
    }

    private TrainingExerciseResponse toExerciseResponse(TrainingExercise trainingExercise) {
        List<TrainingSetResponse> sets = trainingSetRepository
                .findByTrainingExerciseIdOrderBySetNumberAscIdAsc(trainingExercise.getId()).stream()
                .map(this::toSetResponse)
                .toList();

        return new TrainingExerciseResponse(
                trainingExercise.getId(),
                trainingExercise.getExerciseName(),
                trainingExercise.getMemo(),
                sets);
    }

    private TrainingSetResponse toSetResponse(TrainingSet trainingSet) {
        return new TrainingSetResponse(
                trainingSet.getId(),
                trainingSet.getSetNumber(),
                trainingSet.getWeightKg(),
                trainingSet.getReps(),
                trainingSet.getMemo());
    }

    private ResponseStatusException notFound(String resourceName, Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, resourceName + " not found: " + id);
    }
}
