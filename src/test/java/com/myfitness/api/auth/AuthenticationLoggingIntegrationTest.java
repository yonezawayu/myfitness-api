package com.myfitness.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfitness.api.auth.dto.LoginRequest;
import com.myfitness.api.auth.dto.LoginResponse;
import com.myfitness.api.user.dto.UserCreateRequest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication-logging-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.flyway.enabled=false",
        "spring.mvc.log-request-details=false",
        "logging.level.org.springframework.web=TRACE",
        "logging.level.org.springframework.security=TRACE",
        "logging.level.org.hibernate.SQL=OFF",
        "jwt.secret=TEST_ONLY_KEY_FOR_AUTHENTICATION_LOGGING_1234567890",
        "jwt.expiration-minutes=60",
        "jwt.issuer=myfitness-api"
})
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
@Transactional
class AuthenticationLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void authenticationFlowsDoNotExposeCredentialsInLogs(CapturedOutput output) throws Exception {
        String uniqueValue = UUID.randomUUID().toString();
        String email = "log-test-" + uniqueValue + "@example.com";
        String password = "Password-" + uniqueValue;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserCreateRequest(email, password))))
                .andExpect(status().isCreated());

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        String accessToken = response.get("accessToken").asText();

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        MvcResult failedLogin = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, password + "-incorrect"))))
                .andReturn();

        assertThat(failedLogin.getResponse().getStatus() >= 400)
                .as("invalid credentials must not authenticate")
                .isTrue();

        String logs = output.getAll();
        assertSecretIsAbsent(logs, password, "password");
        assertSecretIsAbsent(logs, password + "-incorrect", "invalid password");
        assertSecretIsAbsent(logs, accessToken, "JWT");
        assertSecretIsAbsent(logs, "Bearer " + accessToken, "Authorization header");
    }

    @Test
    void authenticationDtoToStringMethodsRedactSecrets() {
        String password = "dto-password-sentinel";
        String token = "dto-jwt-sentinel";
        String email = "dto-email-sentinel@example.com";

        String loginRequest = new LoginRequest(email, password).toString();
        String userCreateRequest = new UserCreateRequest(email, password).toString();
        String loginResponse = new LoginResponse(token, "Bearer", 3600).toString();

        assertSecretIsAbsent(loginRequest, email, "login request email");
        assertSecretIsAbsent(userCreateRequest, email, "registration email");
        assertSecretIsAbsent(loginRequest, password, "login request password");
        assertSecretIsAbsent(userCreateRequest, password, "registration password");
        assertSecretIsAbsent(loginResponse, token, "login response JWT");
        assertThat(loginRequest).contains("[REDACTED]");
        assertThat(userCreateRequest).contains("[REDACTED]");
        assertThat(loginResponse).contains("[REDACTED]");
    }

    private void assertSecretIsAbsent(String text, String secret, String description) {
        assertThat(text.contains(secret))
                .as("logs must not contain %s", description)
                .isFalse();
    }
}
