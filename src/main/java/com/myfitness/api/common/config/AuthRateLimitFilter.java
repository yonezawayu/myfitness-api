package com.myfitness.api.common.config;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/** Added only to the Security chain, not auto-registered as a second servlet filter. */
public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final AuthRateLimiter limiter;
    private final RequestMatcher login;
    private final RequestMatcher registration;
    private final ClientIpResolver clientIpResolver;

    public AuthRateLimitFilter(AuthRateLimiter limiter, RequestMatcher login, RequestMatcher registration,
                               ClientIpResolver clientIpResolver) {
        this.limiter = limiter;
        this.login = login;
        this.registration = registration;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Share MVC-aware matchers with the permitAll rules. Never decode paths independently.
        boolean registering = registration.matches(request);
        if (registering || login.matches(request)) {
            String clientIp;
            try {
                clientIp = clientIpResolver.resolve(request);
            } catch (ClientIpResolver.InvalidForwardingHeaderException exception) {
                response.setStatus(400);
                response.setHeader("Cache-Control", "no-store");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":\"INVALID_FORWARDING_HEADER\",\"message\":\"リクエストを確認してください。\"}");
                return;
            }
            long retry = limiter.retryAfter(clientIp, registering);
            if (retry > 0) {
                response.setStatus(429);
                response.setHeader("Retry-After", Long.toString(retry));
                response.setHeader("Cache-Control", "no-store");
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"code\":\"RATE_LIMITED\",\"message\":\"しばらく待ってから再試行してください。\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

}
