package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.myfitness.api.auth.service.AuthService;
import com.myfitness.api.auth.dto.LoginResponse;
import com.myfitness.api.user.service.UserService;
import com.myfitness.api.user.dto.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.address=127.0.0.1",
    "spring.datasource.url=jdbc:h2:mem:rate-limit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "jwt.secret=TEST_ONLY_RATE_LIMIT_KEY_123456789012345678901234567890",
    "myfitness.auth-rate-limit.login-attempts=2", "myfitness.auth-rate-limit.registration-attempts=1"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class AuthRateLimitIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired AuthRateLimiter limiter;
    @LocalServerPort int serverPort;
    private static final AtomicInteger clientSequence = new AtomicInteger(20);
    @MockBean AuthService auth;
    @MockBean UserService users;
    @BeforeEach void mocks() {
        when(auth.login(any())).thenReturn(new LoginResponse("test-only-token", "Bearer", 3600));
        when(users.create(any())).thenReturn(new UserResponse(1L, "test@example.invalid", "USER"));
    }
    private MockHttpServletRequestBuilder attempt(String path, String ip) {
        return post(path).with(r -> { r.setRemoteAddr(ip); return r; }).contentType("application/json")
            .content("{\"email\":\"test@example.invalid\",\"password\":\"test-only-password\"}");
    }
    private String freshClient() { return "192.0.2." + clientSequence.getAndIncrement(); }

    private MockHttpServletRequestBuilder rawAttempt(String rawPath, String servletPath, String context, String ip) {
        // URI overload preserves percent escapes; emulate the container's decoded servletPath.
        return post(URI.create(context + rawPath)).contextPath(context)
            .with(r -> { r.setRemoteAddr(ip); r.setServletPath(servletPath); return r; })
            .contentType("application/json")
            .content("{\"email\":\"test@example.invalid\",\"password\":\"test-only-password\"}");
    }

    @ParameterizedTest
    @CsvSource(value = {
        "/auth/%6Cogin|", "/auth/%6cogin|", "/auth/log%69n|", "/%61uth/login|",
        "/auth/%6Cogin|/fitness", "/%61uth/log%69n|/fitness"
    }, delimiter = '|')
    void encodedLoginSharesNormalUrlQuota(String encoded, String context) throws Exception {
        if (context == null) context = "";
        String ip = freshClient();
        mvc.perform(rawAttempt("/auth/login", "/auth/login", context, ip)).andExpect(status().isOk());
        mvc.perform(rawAttempt(encoded, "/auth/login", context, ip)).andExpect(status().isOk());
        mvc.perform(rawAttempt(encoded, "/auth/login", context, ip))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
            .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(rawAttempt("/auth/login", "/auth/login", context, ip)).andExpect(status().isTooManyRequests());
    }

    @ParameterizedTest
    @CsvSource(value = {"/%75sers|", "/us%65rs|", "/%75sers|/fitness", "/us%65rs|/fitness"}, delimiter = '|')
    void encodedRegistrationSharesQuotaInBothDirections(String encoded, String context) throws Exception {
        if (context == null) context = "";
        String ip = freshClient();
        mvc.perform(rawAttempt("/users", "/users", context, ip)).andExpect(status().isCreated());
        mvc.perform(rawAttempt(encoded, "/users", context, ip))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"));
        String reverseIp = freshClient();
        mvc.perform(rawAttempt(encoded, "/users", context, reverseIp)).andExpect(status().isCreated());
        mvc.perform(rawAttempt("/users", "/users", context, reverseIp)).andExpect(status().isTooManyRequests());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/AUTH/LOGIN", "/USERS", "/auth/login/", "/users/", "/auth//login", "/users//",
        "/auth/login;x=1", "/users;x=1", "/auth/./login", "/auth/x/../login", "/users/../users",
        "/auth/%256Cogin", "/%2575sers", "/auth%2Flogin", "/auth/login%2f", "/auth/login%3Bx=1",
        "/users%3Bx=1", "/auth/%2e/login", "/auth/x/%2e%2e/login"})
    void invalidAlternativePathsCannotReachAuthenticationControllers(String rawPath) throws Exception {
        String ip = freshClient();
        clearInvocations(auth, users);
        int code = mvc.perform(rawAttempt(rawPath, rawPath, "", ip)).andReturn().getResponse().getStatus();
        assertThat(code).isBetween(400, 499);
        verifyNoInteractions(auth, users);
    }

    @Test void encodedUrlsRecoverAfterTheSameWindowExpires() throws Exception {
        Clock original = (Clock) ReflectionTestUtils.getField(limiter, "clock");
        Clock controlled = org.mockito.Mockito.mock(Clock.class);
        long now = System.currentTimeMillis() + 1000000;
        when(controlled.millis()).thenReturn(now);
        ReflectionTestUtils.setField(limiter, "clock", controlled);
        try {
            String ip = freshClient();
            mvc.perform(rawAttempt("/auth/login", "/auth/login", "", ip)).andExpect(status().isOk());
            mvc.perform(rawAttempt("/auth/%6Cogin", "/auth/login", "", ip)).andExpect(status().isOk());
            mvc.perform(rawAttempt("/auth/%6Cogin", "/auth/login", "", ip)).andExpect(status().isTooManyRequests());
            mvc.perform(rawAttempt("/users", "/users", "", ip)).andExpect(status().isCreated());
            mvc.perform(rawAttempt("/%75sers", "/users", "", ip)).andExpect(status().isTooManyRequests());
            when(controlled.millis()).thenReturn(now + 300000);
            mvc.perform(rawAttempt("/auth/%6Cogin", "/auth/login", "", ip)).andExpect(status().isOk());
            mvc.perform(rawAttempt("/%75sers", "/users", "", ip)).andExpect(status().isCreated());
        } finally { ReflectionTestUtils.setField(limiter, "clock", original); }
    }

    @Test void separateIpv4ClientsKeepIndependentQuotas() throws Exception {
        String first = freshClient();
        String second = freshClient();
        for (int i = 0; i < 2; i++) {
            mvc.perform(attempt("/auth/login", first)).andExpect(status().isOk());
            mvc.perform(attempt("/auth/login", second)).andExpect(status().isOk());
        }
        mvc.perform(rawAttempt("/auth/%6Cogin", "/auth/login", "", first)).andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", second)).andExpect(status().isTooManyRequests());
    }

    @Test void actualIsolatedTomcatSharesEncodedAndNormalUrlQuotas() throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        assertThat(send(client, "/auth/login").statusCode()).isEqualTo(200);
        assertThat(send(client, "/auth/%6Cogin").statusCode()).isEqualTo(200);
        var blocked = send(client, "/auth/log%69n");
        assertThat(blocked.statusCode()).isEqualTo(429);
        assertThat(blocked.headers().firstValue("Retry-After")).isPresent();
        assertThat(blocked.headers().firstValue("Cache-Control")).contains("no-store");
        assertThat(send(client, "/%75sers").statusCode()).isEqualTo(201);
        assertThat(send(client, "/users").statusCode()).isEqualTo(429);
    }

    private HttpResponse<Void> send(HttpClient client, String rawPath) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + serverPort + rawPath))
            .timeout(Duration.ofSeconds(10)).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"email\":\"test@example.invalid\",\"password\":\"test-only-password\"}")).build(),
            HttpResponse.BodyHandlers.discarding());
    }
    @Test void normalLoginThen429DoesNotLeakSecrets(CapturedOutput output) throws Exception {
        mvc.perform(attempt("/auth/login", "192.0.2.1")).andExpect(status().isOk());
        mvc.perform(attempt("/auth/login", "192.0.2.1")).andExpect(status().isOk());
        var response = mvc.perform(attempt("/auth/login", "192.0.2.1").header("Authorization", "Bearer invalid-sentinel")
                .header("Origin", "http://localhost:3000"))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.code").value("RATE_LIMITED")).andReturn().getResponse();
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isBetween(1, 300);
        assertThat(response.getContentAsString()).doesNotContain("192.0.2.1", "password", "Authorization", "test@example");
        assertThat(output.getAll()).doesNotContain("test-only-password", "invalid-sentinel", "test-only-token");
    }
    @Test void registrationHasIndependentLimitAndOtherApisAreUnaffected() throws Exception {
        mvc.perform(attempt("/users", "192.0.2.2")).andExpect(status().isCreated());
        mvc.perform(attempt("/users", "192.0.2.2")).andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", "192.0.2.2")).andExpect(status().isOk());
        mvc.perform(get("/users/me").with(r -> { r.setRemoteAddr("192.0.2.2"); return r; }))
            .andExpect(status().isUnauthorized());
    }
    @Test void forwardedHeadersCannotChangeTheLimitAndIpv6IsCanonicalized() throws Exception {
        mvc.perform(attempt("/auth/login", "2001:db8::1").header("X-Forwarded-For", "192.0.2.10"))
            .andExpect(status().isOk());
        mvc.perform(attempt("/auth/login", "2001:0db8:0:0:0:0:0:1").header("X-Real-IP", "192.0.2.11"))
            .andExpect(status().isOk());
        mvc.perform(rawAttempt("/auth/%6Cogin", "/auth/login", "", "2001:db8::1")
            .header("X-Forwarded-For", "192.0.2.12").header("Forwarded", "for=192.0.2.13"))
            .andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", "2001:db8::2")).andExpect(status().isOk());
    }
    @Test void preflightDoesNotConsumeLoginQuota() throws Exception {
        for (int i = 0; i < 3; i++) mvc.perform(options("/auth/login")
            .header("Origin", "http://localhost:3000").header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk());
        mvc.perform(attempt("/auth/login", "192.0.2.4")).andExpect(status().isOk());
    }
    @Test void developmentKeepsOpenApiAvailable() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.openapi").exists());
    }
    @Test void failedLoginAttemptsAlsoConsumeQuota() throws Exception {
        when(auth.login(any())).thenThrow(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.UNAUTHORIZED, "Unauthorized"));
        mvc.perform(attempt("/auth/login", "192.0.2.5")).andExpect(status().isUnauthorized());
        mvc.perform(attempt("/auth/login", "192.0.2.5")).andExpect(status().isUnauthorized());
        mvc.perform(attempt("/auth/login", "192.0.2.5")).andExpect(status().isTooManyRequests());
    }
}
