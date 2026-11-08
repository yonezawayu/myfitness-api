package com.myfitness.api.common.config;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public AuthRateLimitFilter(AuthRateLimiter limiter, RequestMatcher login, RequestMatcher registration) {
        this.limiter = limiter;
        this.login = login;
        this.registration = registration;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Share MVC-aware matchers with the permitAll rules. Never decode paths independently.
        boolean registering = registration.matches(request);
        if (registering || login.matches(request)) {
            long retry = limiter.retryAfter(normalize(request.getRemoteAddr()), registering);
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

    private static String normalize(String address) {
        // Only numeric socket addresses; never resolve attacker-controlled DNS names.
        if (address == null || address.length() > 64 || !address.matches("[0-9a-fA-F:.]+")) return "unknown";
        if (!address.contains(":")) {
            String[] octets = address.split("\\.", -1);
            if (octets.length != 4) return "unknown";
            for (String octet : octets) {
                if (!octet.matches("[0-9]{1,3}") || Integer.parseInt(octet) > 255) return "unknown";
            }
            return Integer.parseInt(octets[0]) + "." + Integer.parseInt(octets[1]) + "."
                    + Integer.parseInt(octets[2]) + "." + Integer.parseInt(octets[3]);
        }
        try { return InetAddress.getByName(address).getHostAddress(); }
        catch (UnknownHostException exception) { return "unknown"; }
    }
}
