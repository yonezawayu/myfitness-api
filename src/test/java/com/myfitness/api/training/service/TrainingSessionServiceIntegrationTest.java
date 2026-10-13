package com.myfitness.api.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.training.dto.TrainingExerciseRequest;
import com.myfitness.api.training.dto.TrainingExerciseResponse;
import com.myfitness.api.training.dto.TrainingSessionRequest;
import com.myfitness.api.training.dto.TrainingSessionResponse;
import com.myfitness.api.training.dto.TrainingSetRequest;
import com.myfitness.api.training.dto.TrainingSetResponse;
import com.myfitness.api.training.repository.TrainingExerciseRepository;
import com.myfitness.api.training.repository.TrainingSessionRepository;
import com.myfitness.api.training.repository.TrainingSetRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import(TrainingSessionService.class)
class TrainingSessionServiceIntegrationTest {

    @Autowired
    private TrainingSessionService trainingSessionService;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private TrainingExerciseRepository trainingExerciseRepository;

    @Autowired
    private TrainingSetRepository trainingSetRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void sessionCanBeCreatedFetchedUpdatedAndDeleted() {
        TrainingSessionResponse created = createSession("before");

        TrainingSessionResponse fetched = trainingSessionService.findSessionById(created.id());
        assertThat(fetched.date()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(fetched.memo()).isEqualTo("before");

        TrainingSessionResponse updated = trainingSessionService.updateSession(
                created.id(),
                new TrainingSessionRequest(LocalDate.of(2026, 10, 14), "after"));
        assertThat(updated.date()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(updated.memo()).isEqualTo("after");

        trainingSessionService.deleteSession(created.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(created.id())).isEmpty();
    }

    @Test
    void exerciseCanBeAddedAndDeleted() {
        TrainingSessionResponse session = createSession(null);
        TrainingExerciseResponse exercise = trainingSessionService.createExercise(
                session.id(),
                new TrainingExerciseRequest("ベンチプレス", "main"));

        assertThat(exercise.exerciseName()).isEqualTo("ベンチプレス");
        assertThat(trainingExerciseRepository.findById(exercise.id())).isPresent();
        flushAndClear();

        trainingSessionService.deleteExercise(session.id(), exercise.id());
        flushAndClear();

        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSessionRepository.findById(session.id())).isPresent();
    }

    @Test
    void setCanBeAddedUpdatedAndDeleted() {
        TrainingSessionResponse session = createSession(null);
        TrainingExerciseResponse exercise = createExercise(session.id());
        TrainingSetResponse created = trainingSessionService.createSet(
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(60), 10, null));

        TrainingSetResponse updated = trainingSessionService.updateSet(
                session.id(),
                exercise.id(),
                created.id(),
                new TrainingSetRequest(2, BigDecimal.valueOf(62.5), 8, "hard"));
        assertThat(updated.setNumber()).isEqualTo(2);
        assertThat(updated.weightKg()).isEqualByComparingTo("62.5");
        assertThat(updated.reps()).isEqualTo(8);
        assertThat(updated.memo()).isEqualTo("hard");
        flushAndClear();

        trainingSessionService.deleteSet(session.id(), exercise.id(), created.id());
        flushAndClear();

        assertThat(trainingSetRepository.findById(created.id())).isEmpty();
    }

    @Test
    void mismatchedParentIdsReturnNotFound() {
        TrainingSessionResponse firstSession = createSession("first");
        TrainingSessionResponse secondSession = createSession("second");
        TrainingExerciseResponse firstExercise = createExercise(firstSession.id());
        TrainingExerciseResponse secondExercise = createExercise(firstSession.id());
        TrainingSetResponse firstSet = trainingSessionService.createSet(
                firstSession.id(),
                firstExercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(50), 10, null));

        assertNotFound(() -> trainingSessionService.findExerciseById(secondSession.id(), firstExercise.id()));
        assertNotFound(() -> trainingSessionService.findSetById(firstSession.id(), secondExercise.id(), firstSet.id()));
    }

    @Test
    void deletingSessionCascadesToExercisesAndSets() {
        TrainingSessionResponse session = createSession(null);
        TrainingExerciseResponse exercise = createExercise(session.id());
        TrainingSetResponse trainingSet = trainingSessionService.createSet(
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(70), 5, null));
        flushAndClear();

        trainingSessionService.deleteSession(session.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(session.id())).isEmpty();
        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSetRepository.findById(trainingSet.id())).isEmpty();
    }

    @Test
    void deletingExerciseCascadesToSetsWithoutDeletingSession() {
        TrainingSessionResponse session = createSession(null);
        TrainingExerciseResponse exercise = createExercise(session.id());
        TrainingSetResponse trainingSet = trainingSessionService.createSet(
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(70), 5, null));
        flushAndClear();

        trainingSessionService.deleteExercise(session.id(), exercise.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(session.id())).isPresent();
        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSetRepository.findById(trainingSet.id())).isEmpty();
    }

    private TrainingSessionResponse createSession(String memo) {
        return trainingSessionService.createSession(
                new TrainingSessionRequest(LocalDate.of(2026, 10, 13), memo));
    }

    private TrainingExerciseResponse createExercise(Long sessionId) {
        return trainingSessionService.createExercise(
                sessionId,
                new TrainingExerciseRequest("スクワット", null));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private void assertNotFound(Runnable operation) {
        assertThatThrownBy(operation::run)
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
