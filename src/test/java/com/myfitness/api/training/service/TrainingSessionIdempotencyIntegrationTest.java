package com.myfitness.api.training.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.training.dto.TrainingSessionAggregateRequest;
import com.myfitness.api.training.repository.TrainingExerciseRepository;
import com.myfitness.api.training.repository.TrainingSessionCreationRepository;
import com.myfitness.api.training.repository.TrainingSessionRepository;
import com.myfitness.api.training.repository.TrainingSetRepository;
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-session-idempotency-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "jwt.secret=TEST_ONLY_KEY_FOR_IDEMPOTENCY_TESTS_1234567890"
})
class TrainingSessionIdempotencyIntegrationTest {

    @Autowired
    private TrainingSessionService service;
    @Autowired
    private TrainingSessionRepository sessionRepository;
    @Autowired
    private TrainingExerciseRepository exerciseRepository;
    @Autowired
    private TrainingSetRepository setRepository;
    @Autowired
    private TrainingSessionCreationRepository creationRepository;
    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        creationRepository.deleteAll();
        setRepository.deleteAll();
        exerciseRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(new User("parallel@example.com", "test-hash", "USER"));
    }

    @Test
    void concurrentRequestsWithSameKeyCreateOneAggregate() throws Exception {
        UUID key = UUID.randomUUID();
        TrainingSessionAggregateRequest request = validRequest("parallel");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> invokeTogether(ready, start, key, request));
            var second = executor.submit(() -> invokeTogether(ready, start, key, request));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            TrainingSessionCreationResult firstResult = first.get(10, TimeUnit.SECONDS);
            TrainingSessionCreationResult secondResult = second.get(10, TimeUnit.SECONDS);
            assertThat(firstResult.response().id()).isEqualTo(secondResult.response().id());
            assertThat(List.of(firstResult.replayed(), secondResult.replayed()))
                    .containsExactlyInAnyOrder(false, true);
        } finally {
            executor.shutdownNow();
        }

        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(exerciseRepository.count()).isEqualTo(1);
        assertThat(setRepository.count()).isEqualTo(2);
        assertThat(creationRepository.count()).isEqualTo(1);
    }

    @Test
    void persistenceFailureRollsBackAggregateAndIdempotencyRecord() {
        String invalidDatabaseValue = "x".repeat(300);

        assertThatThrownBy(() -> service.createCompleteSession(
                user.getEmail(), UUID.randomUUID(), requestWithExerciseName(invalidDatabaseValue)))
                .isInstanceOf(RuntimeException.class);

        assertThat(sessionRepository.count()).isZero();
        assertThat(exerciseRepository.count()).isZero();
        assertThat(setRepository.count()).isZero();
        assertThat(creationRepository.count()).isZero();
    }

    @Test
    void deletedSessionCannotBeRecreatedByReusingItsIdempotencyKey() {
        UUID key = UUID.randomUUID();
        TrainingSessionAggregateRequest request = validRequest("deleted");
        TrainingSessionCreationResult created = service.createCompleteSession(user.getEmail(), key, request);

        service.deleteSession(user.getEmail(), created.response().id());

        assertThatThrownBy(() -> service.createCompleteSession(user.getEmail(), key, request))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(sessionRepository.count()).isZero();
        assertThat(exerciseRepository.count()).isZero();
        assertThat(setRepository.count()).isZero();
        assertThat(creationRepository.count()).isEqualTo(1);
    }

    @Test
    void equivalentDecimalRepresentationsProduceTheSameRequestHash() {
        UUID key = UUID.randomUUID();
        TrainingSessionAggregateRequest firstRequest = requestWithWeight(new BigDecimal("80.0"));
        TrainingSessionAggregateRequest retryRequest = requestWithWeight(new BigDecimal("80.00"));

        TrainingSessionCreationResult first = service.createCompleteSession(user.getEmail(), key, firstRequest);
        TrainingSessionCreationResult retry = service.createCompleteSession(user.getEmail(), key, retryRequest);

        assertThat(retry.replayed()).isTrue();
        assertThat(retry.response().id()).isEqualTo(first.response().id());
        assertThat(sessionRepository.count()).isEqualTo(1);
        assertThat(creationRepository.count()).isEqualTo(1);
    }

    private TrainingSessionCreationResult invokeTogether(
            CountDownLatch ready,
            CountDownLatch start,
            UUID key,
            TrainingSessionAggregateRequest request) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent test did not start");
        }
        return service.createCompleteSession(user.getEmail(), key, request);
    }

    private TrainingSessionAggregateRequest validRequest(String memo) {
        return request("スクワット", memo);
    }

    private TrainingSessionAggregateRequest requestWithExerciseName(String exerciseName) {
        return request(exerciseName, null);
    }

    private TrainingSessionAggregateRequest request(String exerciseName, String memo) {
        return request(exerciseName, memo, BigDecimal.valueOf(80));
    }

    private TrainingSessionAggregateRequest requestWithWeight(BigDecimal weight) {
        return request("スクワット", null, weight);
    }

    private TrainingSessionAggregateRequest request(String exerciseName, String memo, BigDecimal weight) {
        return new TrainingSessionAggregateRequest(
                LocalDate.of(2026, 11, 3),
                memo,
                List.of(new TrainingSessionAggregateRequest.Exercise(
                        exerciseName,
                        null,
                        List.of(
                                new TrainingSessionAggregateRequest.Set(1, weight, 8, null),
                                new TrainingSessionAggregateRequest.Set(2, weight, 6, null)))));
    }
}
