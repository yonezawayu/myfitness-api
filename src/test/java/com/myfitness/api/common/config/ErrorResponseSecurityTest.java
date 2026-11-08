package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;
import com.myfitness.api.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

class ErrorResponseSecurityTest {
    @Test void serverErrorDoesNotExposeInternalReason() {
        var response = new GlobalExceptionHandler().handleResponseStatus(
            new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "internal-private-sentinel"),
            new MockHttpServletRequest("GET", "/users/me"));
        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).isEqualTo("Internal Server Error");
    }
}
