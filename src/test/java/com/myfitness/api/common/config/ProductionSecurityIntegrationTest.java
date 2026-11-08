package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:production-security;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "jwt.secret=TEST_ONLY_PRODUCTION_SECURITY_KEY_123456789012345678901234567890",
    "myfitness.cors.allowed-origins=https://app.example.invalid"
})
@ActiveProfiles("prod")
@AutoConfigureMockMvc
class ProductionSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired Environment environment;
    @Autowired RequestMappingHandlerMapping mappings;
    @Test void openApiAndSwaggerAutoConfigurationAreDisabled() {
        assertThat(environment.getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("server.forward-headers-strategy")).isEqualTo("none");
        assertThat(mappings.getHandlerMethods().values()).noneMatch(h -> h.getBeanType().getName().startsWith("org.springdoc"));
    }
    @Test @WithMockUser void authenticatedUsersCannotReadSwaggerOrSchema() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
            "/swagger-ui/index.html", "/swagger-ui.html", "/webjars/swagger-ui/index.html", "/swagger-resources"})
            mvc.perform(get(path)).andExpect(status().isForbidden());
    }
    @Test void anonymousUsersCannotReadSchema() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isUnauthorized());
    }
    @Test void bothProductionAliasesRejectExplicitDocsReEnable() {
        for (String profile : new String[]{"prod", "production"}) {
            var env = new MockEnvironment().withProperty("springdoc.api-docs.enabled", "false")
                .withProperty("springdoc.swagger-ui.enabled", "false");
            env.setActiveProfiles(profile);
            assertThatCode(() -> new SecurityConfig("https://app.example.invalid", env)).doesNotThrowAnyException();
            env.setProperty("springdoc.api-docs.enabled", "true");
            assertThatThrownBy(() -> new SecurityConfig("https://app.example.invalid", env))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
