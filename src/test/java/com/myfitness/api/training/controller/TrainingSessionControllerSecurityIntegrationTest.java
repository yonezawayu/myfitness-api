package com.myfitness.api.training.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfitness.api.auth.jwt.JwtProvider;
import com.myfitness.api.training.dto.TrainingExerciseRequest;
import com.myfitness.api.training.dto.TrainingExerciseResponse;
import com.myfitness.api.training.dto.TrainingSessionRequest;
import com.myfitness.api.training.dto.TrainingSessionResponse;
import com.myfitness.api.training.dto.TrainingSetRequest;
import com.myfitness.api.training.dto.TrainingSetResponse;
import com.myfitness.api.training.repository.TrainingExerciseRepository;
import com.myfitness.api.training.repository.TrainingSessionRepository;
import com.myfitness.api.training.repository.TrainingSetRepository;
import com.myfitness.api.training.service.TrainingSessionService;
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-session-http-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "jwt.secret=TEST_ONLY_KEY_FOR_HTTP_SECURITY_TESTS_1234567890"
})
@AutoConfigureMockMvc
@Transactional
class TrainingSessionControllerSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainingSessionRepository trainingSessionRepository;

    @Autowired
    private TrainingExerciseRepository trainingExerciseRepository;

    @Autowired
    private TrainingSetRepository trainingSetRepository;

    @Autowired
    private TrainingSessionService trainingSessionService;

    private User userA;
    private User userB;
    private String userAToken;
    private String userBToken;

    @BeforeEach
    void setUpUsersAndTokens() {
        userA = userRepository.save(new User("http-user-a@example.com", "test-hash", "USER"));
        userB = userRepository.save(new User("http-user-b@example.com", "test-hash", "USER"));
        userAToken = generateToken(userA);
        userBToken = generateToken(userB);
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/training-sessions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sessionHttpOperationsAreIsolatedByAuthenticatedUser() throws Exception {
        TrainingSessionResponse sessionA = createSession(userA, "user-a");
        TrainingSessionResponse sessionB = createSession(userB, "user-b");

        mockMvc.perform(authenticated(get("/training-sessions"), userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(sessionA.id()));

        mockMvc.perform(authenticated(get("/training-sessions/{sessionId}", sessionB.id()), userAToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}", sessionB.id()),
                userAToken,
                new TrainingSessionRequest(LocalDate.of(2026, 10, 14), "unauthorized")))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(delete("/training-sessions/{sessionId}", sessionB.id()), userAToken))
                .andExpect(status().isNotFound());

        assertThat(trainingSessionRepository.findById(sessionB.id()))
                .get()
                .extracting(trainingSession -> trainingSession.getMemo())
                .isEqualTo("user-b");
    }

    @Test
    void exerciseHttpOperationsRejectOtherOwnerAndMismatchedParent() throws Exception {
        TrainingSessionResponse sessionA = createSession(userA, "user-a");
        TrainingSessionResponse sessionB = createSession(userB, "user-b");
        TrainingSessionResponse otherSessionB = createSession(userB, "user-b-other");
        TrainingExerciseResponse exerciseB = createExercise(userB, sessionB.id(), "スクワット");

        mockMvc.perform(jsonRequest(
                post("/training-sessions/{sessionId}/exercises", sessionB.id()),
                userAToken,
                new TrainingExerciseRequest("不正追加", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}",
                sessionB.id(), exerciseB.id()), userAToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}/exercises/{exerciseId}", sessionB.id(), exerciseB.id()),
                userAToken,
                new TrainingExerciseRequest("不正更新", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(delete(
                "/training-sessions/{sessionId}/exercises/{exerciseId}",
                sessionB.id(), exerciseB.id()), userAToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}",
                otherSessionB.id(), exerciseB.id()), userBToken))
                .andExpect(status().isNotFound());

        assertThat(trainingSessionRepository.findById(sessionA.id())).isPresent();
        assertThat(trainingExerciseRepository.findById(exerciseB.id()))
                .get()
                .extracting(exercise -> exercise.getExerciseName())
                .isEqualTo("スクワット");
        assertThat(trainingExerciseRepository.count()).isEqualTo(1);
    }

    @Test
    void setHttpOperationsRejectOtherOwnerAndMismatchedParent() throws Exception {
        TrainingSessionResponse sessionB = createSession(userB, "user-b");
        TrainingExerciseResponse exerciseB = createExercise(userB, sessionB.id(), "ベンチプレス");
        TrainingExerciseResponse otherExerciseB = createExercise(userB, sessionB.id(), "デッドリフト");
        TrainingSetResponse setB = createSet(userB, sessionB.id(), exerciseB.id());

        mockMvc.perform(jsonRequest(
                post("/training-sessions/{sessionId}/exercises/{exerciseId}/sets",
                        sessionB.id(), exerciseB.id()),
                userAToken,
                new TrainingSetRequest(2, BigDecimal.valueOf(82.5), 5, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                sessionB.id(), exerciseB.id(), setB.id()), userAToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                        sessionB.id(), exerciseB.id(), setB.id()),
                userAToken,
                new TrainingSetRequest(1, BigDecimal.valueOf(100), 3, "unauthorized")))
                .andExpect(status().isNotFound());
        mockMvc.perform(authenticated(delete(
                "/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                sessionB.id(), exerciseB.id(), setB.id()), userAToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                sessionB.id(), otherExerciseB.id(), setB.id()), userBToken))
                .andExpect(status().isNotFound());

        assertThat(trainingSetRepository.findById(setB.id()))
                .get()
                .satisfies(trainingSet -> {
                    assertThat(trainingSet.getSetNumber()).isEqualTo(1);
                    assertThat(trainingSet.getWeightKg()).isEqualByComparingTo("80.0");
                    assertThat(trainingSet.getReps()).isEqualTo(8);
                    assertThat(trainingSet.getMemo()).isNull();
                });
        assertThat(trainingSetRepository.count()).isEqualTo(1);
    }

    @Test
    void ownerCanCompleteSessionExerciseAndSetCrudThroughHttp() throws Exception {
        JsonNode session = responseJson(mockMvc.perform(jsonRequest(
                post("/training-sessions"),
                userAToken,
                new TrainingSessionRequest(LocalDate.of(2026, 10, 14), "created")))
                .andExpect(status().isCreated())
                .andReturn());
        long sessionId = session.get("id").asLong();

        JsonNode exercise = responseJson(mockMvc.perform(jsonRequest(
                post("/training-sessions/{sessionId}/exercises", sessionId),
                userAToken,
                new TrainingExerciseRequest("スクワット", "created")))
                .andExpect(status().isCreated())
                .andReturn());
        long exerciseId = exercise.get("id").asLong();

        JsonNode trainingSet = responseJson(mockMvc.perform(jsonRequest(
                post("/training-sessions/{sessionId}/exercises/{exerciseId}/sets", sessionId, exerciseId),
                userAToken,
                new TrainingSetRequest(1, BigDecimal.valueOf(60), 10, "created")))
                .andExpect(status().isCreated())
                .andReturn());
        long setId = trainingSet.get("id").asLong();

        mockMvc.perform(authenticated(get("/training-sessions/{sessionId}", sessionId), userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exercises[0].sets[0].id").value(setId));
        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}",
                sessionId, exerciseId), userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exerciseId));
        mockMvc.perform(authenticated(get(
                "/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                sessionId, exerciseId, setId), userAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(setId));
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}", sessionId),
                userAToken,
                new TrainingSessionRequest(LocalDate.of(2026, 10, 13), "updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memo").value("updated"));
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}/exercises/{exerciseId}", sessionId, exerciseId),
                userAToken,
                new TrainingExerciseRequest("フロントスクワット", "updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseName").value("フロントスクワット"));
        mockMvc.perform(jsonRequest(
                put("/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                        sessionId, exerciseId, setId),
                userAToken,
                new TrainingSetRequest(1, BigDecimal.valueOf(62.5), 8, "updated")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(62.5))
                .andExpect(jsonPath("$.reps").value(8));

        mockMvc.perform(authenticated(delete(
                "/training-sessions/{sessionId}/exercises/{exerciseId}/sets/{setId}",
                sessionId, exerciseId, setId), userAToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(authenticated(delete(
                "/training-sessions/{sessionId}/exercises/{exerciseId}",
                sessionId, exerciseId), userAToken))
                .andExpect(status().isNoContent());
        mockMvc.perform(authenticated(delete("/training-sessions/{sessionId}", sessionId), userAToken))
                .andExpect(status().isNoContent());

        assertThat(trainingSetRepository.findById(setId)).isEmpty();
        assertThat(trainingExerciseRepository.findById(exerciseId)).isEmpty();
        assertThat(trainingSessionRepository.findById(sessionId)).isEmpty();
    }

    private TrainingSessionResponse createSession(User user, String memo) {
        return trainingSessionService.createSession(
                user.getEmail(),
                new TrainingSessionRequest(LocalDate.of(2026, 10, 14), memo));
    }

    private TrainingExerciseResponse createExercise(User user, Long sessionId, String exerciseName) {
        return trainingSessionService.createExercise(
                user.getEmail(),
                sessionId,
                new TrainingExerciseRequest(exerciseName, null));
    }

    private TrainingSetResponse createSet(User user, Long sessionId, Long exerciseId) {
        return trainingSessionService.createSet(
                user.getEmail(),
                sessionId,
                exerciseId,
                new TrainingSetRequest(1, BigDecimal.valueOf(80), 8, null));
    }

    private String generateToken(User user) {
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                user.getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return jwtProvider.generate(authentication);
    }

    private MockHttpServletRequestBuilder authenticated(
            MockHttpServletRequestBuilder request,
            String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder jsonRequest(
            MockHttpServletRequestBuilder request,
            String token,
            Object body) throws Exception {
        return authenticated(request, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body));
    }

    private JsonNode responseJson(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
