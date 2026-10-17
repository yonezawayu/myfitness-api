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
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
public class TrainingSessionService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingExerciseRepository trainingExerciseRepository;
    private final TrainingSetRepository trainingSetRepository;
    private final UserRepository userRepository;

    public TrainingSessionService(
            TrainingSessionRepository trainingSessionRepository,
            TrainingExerciseRepository trainingExerciseRepository,
            TrainingSetRepository trainingSetRepository,
            UserRepository userRepository) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.trainingExerciseRepository = trainingExerciseRepository;
        this.trainingSetRepository = trainingSetRepository;
        this.userRepository = userRepository;
    }

    public List<TrainingSessionResponse> findAllSessions(String userEmail) {
        User user = findAuthenticatedUser(userEmail);
        return trainingSessionRepository.findAllByUserIdOrderByDateDescIdDesc(user.getId()).stream()
                .map(this::toSessionResponse)
                .toList();
    }

    public TrainingSessionResponse findSessionById(String userEmail, Long sessionId) {
        User user = findAuthenticatedUser(userEmail);
        return toSessionResponse(findSession(user.getId(), sessionId));
    }

    @Transactional
    public TrainingSessionResponse createSession(String userEmail, TrainingSessionRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingSession trainingSession = new TrainingSession(request.date(), request.memo(), user);
        return toSessionResponse(trainingSessionRepository.save(trainingSession));
    }

    @Transactional
    public TrainingSessionResponse updateSession(
            String userEmail,
            Long sessionId,
            TrainingSessionRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingSession trainingSession = findSession(user.getId(), sessionId);
        trainingSession.setDate(request.date());
        trainingSession.setMemo(request.memo());
        return toSessionResponse(trainingSession);
    }

    @Transactional
    public void deleteSession(String userEmail, Long sessionId) {
        User user = findAuthenticatedUser(userEmail);
        trainingSessionRepository.delete(findSession(user.getId(), sessionId));
    }

    public List<TrainingExerciseResponse> findAllExercises(String userEmail, Long sessionId) {
        User user = findAuthenticatedUser(userEmail);
        findSession(user.getId(), sessionId);
        return trainingExerciseRepository.findAllOwnedBySession(sessionId, user.getId()).stream()
                .map(this::toExerciseResponse)
                .toList();
    }

    public TrainingExerciseResponse findExerciseById(String userEmail, Long sessionId, Long exerciseId) {
        User user = findAuthenticatedUser(userEmail);
        return toExerciseResponse(findExercise(user.getId(), sessionId, exerciseId));
    }

    @Transactional
    public TrainingExerciseResponse createExercise(
            String userEmail,
            Long sessionId,
            TrainingExerciseRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingSession trainingSession = findSession(user.getId(), sessionId);
        TrainingExercise trainingExercise = new TrainingExercise(request.exerciseName(), request.memo());
        trainingSession.addExercise(trainingExercise);
        return toExerciseResponse(trainingExerciseRepository.save(trainingExercise));
    }

    @Transactional
    public TrainingExerciseResponse updateExercise(
            String userEmail,
            Long sessionId,
            Long exerciseId,
            TrainingExerciseRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingExercise trainingExercise = findExercise(user.getId(), sessionId, exerciseId);
        trainingExercise.setExerciseName(request.exerciseName());
        trainingExercise.setMemo(request.memo());
        return toExerciseResponse(trainingExercise);
    }

    @Transactional
    public void deleteExercise(String userEmail, Long sessionId, Long exerciseId) {
        User user = findAuthenticatedUser(userEmail);
        trainingExerciseRepository.delete(findExercise(user.getId(), sessionId, exerciseId));
    }

    public List<TrainingSetResponse> findAllSets(String userEmail, Long sessionId, Long exerciseId) {
        User user = findAuthenticatedUser(userEmail);
        findExercise(user.getId(), sessionId, exerciseId);
        return trainingSetRepository.findAllOwnedByExercise(sessionId, exerciseId, user.getId()).stream()
                .map(this::toSetResponse)
                .toList();
    }

    public TrainingSetResponse findSetById(
            String userEmail,
            Long sessionId,
            Long exerciseId,
            Long setId) {
        User user = findAuthenticatedUser(userEmail);
        return toSetResponse(findSet(user.getId(), sessionId, exerciseId, setId));
    }

    @Transactional
    public TrainingSetResponse createSet(
            String userEmail,
            Long sessionId,
            Long exerciseId,
            TrainingSetRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingExercise trainingExercise = findExercise(user.getId(), sessionId, exerciseId);
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
            String userEmail,
            Long sessionId,
            Long exerciseId,
            Long setId,
            TrainingSetRequest request) {
        User user = findAuthenticatedUser(userEmail);
        TrainingSet trainingSet = findSet(user.getId(), sessionId, exerciseId, setId);
        trainingSet.setSetNumber(request.setNumber());
        trainingSet.setWeightKg(request.weightKg());
        trainingSet.setReps(request.reps());
        trainingSet.setMemo(request.memo());
        return toSetResponse(trainingSet);
    }

    @Transactional
    public void deleteSet(String userEmail, Long sessionId, Long exerciseId, Long setId) {
        User user = findAuthenticatedUser(userEmail);
        trainingSetRepository.delete(findSet(user.getId(), sessionId, exerciseId, setId));
    }

    private User findAuthenticatedUser(String userEmail) {
        return userRepository.findByEmail(userEmail.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found"));
    }

    private TrainingSession findSession(Long userId, Long sessionId) {
        return trainingSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> notFound("TrainingSession", sessionId));
    }

    private TrainingExercise findExercise(Long userId, Long sessionId, Long exerciseId) {
        return trainingExerciseRepository.findOwnedById(sessionId, exerciseId, userId)
                .orElseThrow(() -> notFound("TrainingExercise", exerciseId));
    }

    private TrainingSet findSet(Long userId, Long sessionId, Long exerciseId, Long setId) {
        return trainingSetRepository.findOwnedById(sessionId, exerciseId, setId, userId)
                .orElseThrow(() -> notFound("TrainingSet", setId));
    }

    private TrainingSessionResponse toSessionResponse(TrainingSession trainingSession) {
        List<TrainingExerciseResponse> exercises = trainingExerciseRepository
                .findAllOwnedBySession(trainingSession.getId(), trainingSession.getUser().getId()).stream()
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
                .findAllOwnedByExercise(
                        trainingExercise.getTrainingSession().getId(),
                        trainingExercise.getId(),
                        trainingExercise.getTrainingSession().getUser().getId()).stream()
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
