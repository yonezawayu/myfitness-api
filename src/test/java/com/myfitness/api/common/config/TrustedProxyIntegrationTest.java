package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.net.URI;
import com.myfitness.api.auth.dto.LoginResponse;
import com.myfitness.api.auth.service.AuthService;
import com.myfitness.api.user.dto.UserResponse;
import com.myfitness.api.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:trusted-proxy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
    "jwt.secret=TEST_ONLY_PROXY_KEY_123456789012345678901234567890",
    "myfitness.proxy.trusted-cidrs=10.20.0.0/24,2001:db8:ffff::/48",
    "myfitness.auth-rate-limit.login-attempts=2", "myfitness.auth-rate-limit.registration-attempts=1"
})
@AutoConfigureMockMvc
class TrustedProxyIntegrationTest {
    @Autowired MockMvc mvc;
    @MockBean AuthService auth;
    @MockBean UserService users;

    @BeforeEach void mocks() {
        when(auth.login(any())).thenReturn(new LoginResponse("test-only-token", "Bearer", 3600));
        when(users.create(any())).thenReturn(new UserResponse(1L, "test@example.invalid", "USER"));
    }

    private MockHttpServletRequestBuilder attempt(String rawPath, String decoded, String peer, String forwarded) {
        var request = post(URI.create(rawPath)).with(r -> {
            r.setRemoteAddr(peer); r.setServletPath(decoded); return r;
        }).contentType("application/json").content("{\"email\":\"test@example.invalid\",\"password\":\"test-only-password\"}");
        if (forwarded != null) request.header("X-Forwarded-For", forwarded);
        return request;
    }

    @Test void sameClientSharesQuotaAcrossProxiesAndEncodedUrls() throws Exception {
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "203.0.113.1,198.51.100.1"))
            .andExpect(status().isOk());
        mvc.perform(attempt("/auth/%6Cogin", "/auth/login", "2001:db8:ffff::1", "203.0.113.2,198.51.100.1,10.20.0.3"))
            .andExpect(status().isOk());
        var blocked = mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.4", "203.0.113.3,198.51.100.1"))
            .andExpect(status().isTooManyRequests()).andExpect(header().exists("Retry-After"))
            .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse();
        assertThat(Integer.parseInt(blocked.getHeader("Retry-After"))).isBetween(1, 300);
        mvc.perform(attempt("/auth/login", "/auth/login", "198.51.100.1", "192.0.2.200"))
            .andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "198.51.100.2"))
            .andExpect(status().isOk());
    }

    @Test void ipv6EquivalentClientsShareQuotaAndDifferentClientsDoNot() throws Exception {
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "2001:db8::10")).andExpect(status().isOk());
        mvc.perform(attempt("/auth/login", "/auth/login", "2001:db8:ffff::2", "2001:0db8:0:0:0:0:0:10"))
            .andExpect(status().isOk());
        mvc.perform(attempt("/auth/%6Cogin", "/auth/login", "10.20.0.2", "2001:db8::10"))
            .andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "2001:db8::11")).andExpect(status().isOk());
    }

    @Test void registrationUsesSameResolvedClientAndEncodedQuota() throws Exception {
        mvc.perform(attempt("/users", "/users", "10.20.0.2", "198.51.100.3")).andExpect(status().isCreated());
        mvc.perform(attempt("/%75sers", "/users", "10.20.0.3", "203.0.113.1,198.51.100.3"))
            .andExpect(status().isTooManyRequests());
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "198.51.100.3")).andExpect(status().isOk());
    }

    @Test void untrustedCallerCannotRotateAnyForwardingHeadersToBypassQuota() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(attempt("/auth/login", "/auth/login", "192.0.2.4", "198.51.100." + i)
                    .header("X-Real-IP", "203.0.113." + i).header("Forwarded", "for=203.0.113." + i))
                .andExpect(status().is(i < 2 ? 200 : 429));
        }
    }

    @Test void malformedTrustedHeaderIsGeneric400WithoutCallingAuth() throws Exception {
        clearInvocations(auth);
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "invalid-sentinel"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_FORWARDING_HEADER"))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("invalid-sentinel"))));
        verifyNoInteractions(auth);
    }

    @Test void preflightAndProtectedApiKeepTheirSecuritySemantics() throws Exception {
        mvc.perform(options("/auth/login").with(r -> { r.setRemoteAddr("10.20.0.2"); return r; })
                .header("X-Forwarded-For", "malformed").header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type,Idempotency-Key"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(get("/users/me").header("X-Forwarded-For", "198.51.100.6")).andExpect(status().isUnauthorized());
        mvc.perform(attempt("/auth/login", "/auth/login", "10.20.0.2", "198.51.100.6")).andExpect(status().isOk());
    }
}
