package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.cors.CorsConfiguration;

class CorsAllowedOriginsTest {

    @Test
    void localEnvironmentDefaultsToLocalFrontendOnly() {
        SecurityConfig securityConfig = new SecurityConfig("", new MockEnvironment());
        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/training-sessions"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.checkOrigin("http://localhost:3000")).isEqualTo("http://localhost:3000");
        assertThat(configuration.checkOrigin("https://untrusted.example.com")).isNull();
    }

    @Test
    void configuredOriginsAreNormalizedAndRestrictedToTheExplicitList() {
        List<String> origins = CorsAllowedOrigins.resolve(
                " https://app.example.com/, https://admin.example.com ",
                true);
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);

        assertThat(origins).containsExactly("https://app.example.com", "https://admin.example.com");
        assertThat(configuration.checkOrigin("https://app.example.com")).isEqualTo("https://app.example.com");
        assertThat(configuration.checkOrigin("https://untrusted.example.com")).isNull();
    }

    @Test
    void productionRejectsMissingInsecureAndLoopbackOrigins() {
        assertThatThrownBy(() -> CorsAllowedOrigins.resolve("", true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorsAllowedOrigins.resolve("http://app.example.com", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorsAllowedOrigins.resolve("https://localhost:3000", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsWildcardsAndOriginsContainingPaths() {
        assertThatThrownBy(() -> CorsAllowedOrigins.resolve("https://*.example.com", false))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CorsAllowedOrigins.resolve("https://app.example.com/path", false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
