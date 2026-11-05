package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-session-cors-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "myfitness.cors.allowed-origins=http://localhost:3000",
        "jwt.secret=TEST_ONLY_KEY_FOR_CORS_INTEGRATION_TESTS_1234567890"
})
@AutoConfigureMockMvc
class TrainingSessionCorsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsIdempotentCreationPreflightWithoutAuthentication() throws Exception {
        String allowedHeaders = mockMvc.perform(preflight("http://localhost:3000",
                        "Authorization, Content-Type, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andReturn().getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

        assertThat(allowedHeaders).isNotNull();
        assertThat(Arrays.stream(allowedHeaders.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).toList())
                .containsExactlyInAnyOrder("authorization", "content-type", "idempotency-key");
    }

    @Test
    void rejectsUnapprovedOriginEvenWithSupportedHeaders() throws Exception {
        mockMvc.perform(preflight("http://evil.example.com",
                        "Authorization, Content-Type, Idempotency-Key"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
    }

    @Test
    void preservesExistingAuthorizationAndContentTypePreflight() throws Exception {
        String allowedHeaders = mockMvc.perform(preflight("http://localhost:3000", "Authorization, Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andReturn().getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);

        assertThat(allowedHeaders).isNotNull();
        assertThat(Arrays.stream(allowedHeaders.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT)).toList())
                .containsExactlyInAnyOrder("authorization", "content-type");
    }

    private MockHttpServletRequestBuilder preflight(String origin, String requestedHeaders) {
        return options("/training-sessions/complete")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, requestedHeaders);
    }
}
