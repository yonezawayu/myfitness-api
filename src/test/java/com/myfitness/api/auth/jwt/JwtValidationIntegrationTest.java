package com.myfitness.api.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfitness.api.user.entity.User;
import com.myfitness.api.user.repository.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jwt-validation-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "logging.level.org.springframework.web=INFO",
        "logging.level.org.hibernate.SQL=OFF",
        "debug=false",
        "jwt.secret=TEST_ONLY_KEY_FOR_STRICT_JWT_VALIDATION_1234567890",
        "jwt.expiration-minutes=60",
        "jwt.issuer=myfitness-api"
})
@AutoConfigureMockMvc
@Transactional
class JwtValidationIntegrationTest {

    private static final String ISSUER = "myfitness-api";
    private static final String EMAIL = "jwt-validation@example.com";
    private static final String PASSWORD = "test-password-only";
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(
            "TEST_ONLY_KEY_FOR_STRICT_JWT_VALIDATION_1234567890".getBytes(StandardCharsets.UTF_8));
    private static final SecretKey OTHER_KEY = Keys.hmacShaKeyFor(
            "DIFFERENT_TEST_ONLY_KEY_FOR_INVALID_SIGNATURE_123456".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpUser() {
        userRepository.save(new User(EMAIL, passwordEncoder.encode(PASSWORD), "USER"));
    }

    @Test
    void validJwtAuthenticatesSuccessfully() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), EMAIL, SIGNING_KEY, true, true, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void missingIssuerReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(null, futureExpiration(), EMAIL, SIGNING_KEY, false, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void incorrectIssuerReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token("different-issuer", futureExpiration(), EMAIL, SIGNING_KEY,
                true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingExpirationReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, null, EMAIL, SIGNING_KEY, true, false, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredJwtReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, Date.from(Instant.now().minus(1, ChronoUnit.MINUTES)),
                EMAIL, SIGNING_KEY, true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingSubjectReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), null, SIGNING_KEY, true, true, false)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptySubjectReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), "", SIGNING_KEY, true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blankSubjectReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), "   ", SIGNING_KEY, true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidSignatureReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), EMAIL, OTHER_KEY, true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unknownSubjectReturnsUnauthorized() throws Exception {
        mockMvc.perform(authenticatedGet(token(ISSUER, futureExpiration(), "missing-user@example.com",
                SIGNING_KEY, true, true, true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginIssuesJwtWithExpectedIssuerExpirationAndSubject() throws Exception {
        String requestBody = objectMapper.writeValueAsString(new LoginCredentials(EMAIL, PASSWORD));

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        Claims claims = jwtProvider.parse(response.get("accessToken").asText()).getPayload();

        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
        assertThat(claims.getExpiration()).isBeforeOrEqualTo(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder authenticatedGet(String token) {
        return get("/users/me").header("Authorization", "Bearer " + token);
    }

    private Date futureExpiration() {
        return Date.from(Instant.now().plus(10, ChronoUnit.MINUTES));
    }

    private String token(
            String issuer,
            Date expiration,
            String subject,
            SecretKey signingKey,
            boolean includeIssuer,
            boolean includeExpiration,
            boolean includeSubject) {
        var builder = Jwts.builder().issuedAt(Date.from(Instant.now()));

        if (includeIssuer) {
            builder.issuer(issuer);
        }
        if (includeExpiration) {
            builder.expiration(expiration);
        }
        if (includeSubject) {
            builder.claim("sub", subject);
        }

        return builder.signWith(signingKey).compact();
    }

    private record LoginCredentials(String email, String password) {
    }
}
