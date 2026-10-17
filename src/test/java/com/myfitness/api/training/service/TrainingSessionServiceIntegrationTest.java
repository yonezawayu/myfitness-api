package com.myfitness.api.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
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
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
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
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User userA;
    private User userB;

    @BeforeEach
    void setUpUsers() {
        userA = userRepository.save(new User("user-a@example.com", "test-hash", "USER"));
        userB = userRepository.save(new User("user-b@example.com", "test-hash", "USER"));
    }

    @Test
    void sessionCanBeCreatedFetchedUpdatedAndDeletedByOwner() {
        TrainingSessionResponse created = createSession(userA, "before");

        assertThat(trainingSessionRepository.findById(created.id()))
                .get()
                .extracting(session -> session.getUser().getId())
                .isEqualTo(userA.getId());

        TrainingSessionResponse fetched = trainingSessionService.findSessionById(userA.getEmail(), created.id());
        assertThat(fetched.date()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(fetched.memo()).isEqualTo("before");

        TrainingSessionResponse updated = trainingSessionService.updateSession(
                userA.getEmail(),
                created.id(),
                new TrainingSessionRequest(LocalDate.of(2026, 10, 14), "after"));
        assertThat(updated.date()).isEqualTo(LocalDate.of(2026, 10, 14));
        assertThat(updated.memo()).isEqualTo("after");

        trainingSessionService.deleteSession(userA.getEmail(), created.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(created.id())).isEmpty();
    }

    @Test
    void sessionListAndCrudAreIsolatedByOwner() {
        TrainingSessionResponse sessionA = createSession(userA, "user-a");
        TrainingSessionResponse sessionB = createSession(userB, "user-b");

        assertThat(trainingSessionService.findAllSessions(userA.getEmail()))
                .extracting(TrainingSessionResponse::id)
                .containsExactly(sessionA.id());
        assertThat(trainingSessionService.findAllSessions(userB.getEmail()))
                .extracting(TrainingSessionResponse::id)
                .containsExactly(sessionB.id());

        assertNotFound(() -> trainingSessionService.findSessionById(userB.getEmail(), sessionA.id()));
        assertNotFound(() -> trainingSessionService.updateSession(
                userB.getEmail(),
                sessionA.id(),
                new TrainingSessionRequest(LocalDate.of(2026, 10, 15), "unauthorized")));
        assertNotFound(() -> trainingSessionService.deleteSession(userB.getEmail(), sessionA.id()));
        assertNotFound(() -> trainingSessionService.findSessionById(userA.getEmail(), Long.MAX_VALUE));

        flushAndClear();
        assertThat(trainingSessionRepository.findById(sessionA.id()))
                .get()
                .extracting(session -> session.getMemo())
                .isEqualTo("user-a");
    }

    @Test
    void exerciseCanBeAddedAndDeletedByOwner() {
        TrainingSessionResponse session = createSession(userA, null);
        TrainingExerciseResponse exercise = trainingSessionService.createExercise(
                userA.getEmail(),
                session.id(),
                new TrainingExerciseRequest("ベンチプレス", "main"));

        assertThat(exercise.exerciseName()).isEqualTo("ベンチプレス");
        assertThat(trainingExerciseRepository.findById(exercise.id())).isPresent();
        flushAndClear();

        trainingSessionService.deleteExercise(userA.getEmail(), session.id(), exercise.id());
        flushAndClear();

        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSessionRepository.findById(session.id())).isPresent();
    }

    @Test
    void setCanBeAddedUpdatedAndDeletedByOwner() {
        TrainingSessionResponse session = createSession(userA, null);
        TrainingExerciseResponse exercise = createExercise(userA, session.id());
        TrainingSetResponse created = trainingSessionService.createSet(
                userA.getEmail(),
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(60), 10, null));

        TrainingSetResponse updated = trainingSessionService.updateSet(
                userA.getEmail(),
                session.id(),
                exercise.id(),
                created.id(),
                new TrainingSetRequest(2, BigDecimal.valueOf(62.5), 8, "hard"));
        assertThat(updated.setNumber()).isEqualTo(2);
        assertThat(updated.weightKg()).isEqualByComparingTo("62.5");
        assertThat(updated.reps()).isEqualTo(8);
        assertThat(updated.memo()).isEqualTo("hard");
        flushAndClear();

        trainingSessionService.deleteSet(userA.getEmail(), session.id(), exercise.id(), created.id());
        flushAndClear();

        assertThat(trainingSetRepository.findById(created.id())).isEmpty();
    }

    @Test
    void otherUserCannotAccessOrMutateExercisesAndSets() {
        TrainingSessionResponse session = createSession(userA, null);
        TrainingExerciseResponse exercise = createExercise(userA, session.id());
        TrainingSetResponse trainingSet = trainingSessionService.createSet(
                userA.getEmail(),
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(70), 5, null));

        assertNotFound(() -> trainingSessionService.findAllExercises(userB.getEmail(), session.id()));
        assertNotFound(() -> trainingSessionService.findExerciseById(
                userB.getEmail(), session.id(), exercise.id()));
        assertNotFound(() -> trainingSessionService.createExercise(
                userB.getEmail(), session.id(), new TrainingExerciseRequest("不正追加", null)));
        assertNotFound(() -> trainingSessionService.updateExercise(
                userB.getEmail(), session.id(), exercise.id(), new TrainingExerciseRequest("不正更新", null)));
        assertNotFound(() -> trainingSessionService.deleteExercise(
                userB.getEmail(), session.id(), exercise.id()));

        assertNotFound(() -> trainingSessionService.findAllSets(
                userB.getEmail(), session.id(), exercise.id()));
        assertNotFound(() -> trainingSessionService.findSetById(
                userB.getEmail(), session.id(), exercise.id(), trainingSet.id()));
        assertNotFound(() -> trainingSessionService.createSet(
                userB.getEmail(),
                session.id(),
                exercise.id(),
                new TrainingSetRequest(2, BigDecimal.valueOf(72.5), 5, null)));
        assertNotFound(() -> trainingSessionService.updateSet(
                userB.getEmail(),
                session.id(),
                exercise.id(),
                trainingSet.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(80), 3, null)));
        assertNotFound(() -> trainingSessionService.deleteSet(
                userB.getEmail(), session.id(), exercise.id(), trainingSet.id()));

        flushAndClear();
        assertThat(trainingExerciseRepository.findById(exercise.id())).isPresent();
        assertThat(trainingSetRepository.findById(trainingSet.id())).isPresent();
    }

    @Test
    void mismatchedParentIdsReturnNotFound() {
        TrainingSessionResponse firstSession = createSession(userA, "first");
        TrainingSessionResponse secondSession = createSession(userA, "second");
        TrainingExerciseResponse firstExercise = createExercise(userA, firstSession.id());
        TrainingExerciseResponse secondExercise = createExercise(userA, firstSession.id());
        TrainingSetResponse firstSet = trainingSessionService.createSet(
                userA.getEmail(),
                firstSession.id(),
                firstExercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(50), 10, null));

        assertNotFound(() -> trainingSessionService.findExerciseById(
                userA.getEmail(), secondSession.id(), firstExercise.id()));
        assertNotFound(() -> trainingSessionService.findSetById(
                userA.getEmail(), firstSession.id(), secondExercise.id(), firstSet.id()));
    }

    @Test
    void deletingSessionCascadesToExercisesAndSets() {
        TrainingSessionResponse session = createSession(userA, null);
        TrainingExerciseResponse exercise = createExercise(userA, session.id());
        TrainingSetResponse trainingSet = trainingSessionService.createSet(
                userA.getEmail(),
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(70), 5, null));
        flushAndClear();

        trainingSessionService.deleteSession(userA.getEmail(), session.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(session.id())).isEmpty();
        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSetRepository.findById(trainingSet.id())).isEmpty();
    }

    @Test
    void deletingExerciseCascadesToSetsWithoutDeletingSession() {
        TrainingSessionResponse session = createSession(userA, null);
        TrainingExerciseResponse exercise = createExercise(userA, session.id());
        TrainingSetResponse trainingSet = trainingSessionService.createSet(
                userA.getEmail(),
                session.id(),
                exercise.id(),
                new TrainingSetRequest(1, BigDecimal.valueOf(70), 5, null));
        flushAndClear();

        trainingSessionService.deleteExercise(userA.getEmail(), session.id(), exercise.id());
        flushAndClear();

        assertThat(trainingSessionRepository.findById(session.id())).isPresent();
        assertThat(trainingExerciseRepository.findById(exercise.id())).isEmpty();
        assertThat(trainingSetRepository.findById(trainingSet.id())).isEmpty();
    }

    private TrainingSessionResponse createSession(User user, String memo) {
        return trainingSessionService.createSession(
                user.getEmail(),
                new TrainingSessionRequest(LocalDate.of(2026, 10, 13), memo));
    }

    private TrainingExerciseResponse createExercise(User user, Long sessionId) {
        return trainingSessionService.createExercise(
                user.getEmail(),
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
