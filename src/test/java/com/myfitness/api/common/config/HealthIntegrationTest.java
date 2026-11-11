package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "server.address=127.0.0.1",
    "spring.datasource.url=jdbc:h2:mem:health;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "jwt.secret=TEST_ONLY_HEALTH_KEY_123456789012345678901234567890",
    "myfitness.cors.allowed-origins=https://app.example.invalid"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class HealthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ConfigurableApplicationContext context;
    @LocalServerPort int port;
    @SpyBean DataSource dataSource;

    @AfterEach void restoreAvailability() {
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/health", "/health/live", "/health/ready"})
    void anonymousProductionProbesExposeOnlyStatus(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test void startupAndShutdownReadinessRefuseTrafficWithoutFailingLiveness() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/health/ready")).andExpect(status().isServiceUnavailable())
            .andExpect(content().json("{\"status\":\"DOWN\"}", true));
        mvc.perform(get("/health")).andExpect(status().isServiceUnavailable());
        mvc.perform(get("/health/live")).andExpect(status().isOk());
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        mvc.perform(get("/health/ready")).andExpect(status().isOk());
    }

    @Test void brokenLivenessMakesBothProbes503WithoutInternalDetails() throws Exception {
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
        for (String path : new String[]{"/health", "/health/live", "/health/ready"}) {
            mvc.perform(get(path)).andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"status\":\"DOWN\"}", true));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/actuator", "/actuator/env", "/actuator/health", "/actuator/metrics", "/health/details",
        "/health/ready/details", "/users/me"})
    void otherManagementAndProtectedPathsAreNotPublic(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
    }

    @Test void nonGetMethodsAreNotMadePublic() throws Exception {
        mvc.perform(post("/health")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/health/ready")).andExpect(status().isUnauthorized());
    }

    @Test void repeatedProbesDoNotBorrowDatabaseConnections() throws Exception {
        clearInvocations(dataSource);
        for (int i = 0; i < 10; i++) {
            mvc.perform(get("/health/live")).andExpect(status().isOk());
            mvc.perform(get("/health/ready")).andExpect(status().isOk());
        }
        verify(dataSource, never()).getConnection();
        verify(dataSource, never()).getConnection(anyString(), anyString());
    }

    @Test void isolatedHttpHealthEndpointWorksWithoutCredentials() throws Exception {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/health/ready"))
            .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
    }
}
