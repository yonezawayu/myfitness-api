package com.myfitness.api.common.config;

import com.myfitness.api.auth.jwt.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final List<String> allowedOrigins;
        private final boolean production;

        public SecurityConfig(
                        @Value("${myfitness.cors.allowed-origins:}") String configuredAllowedOrigins,
                        Environment environment) {
                this.production = environment.acceptsProfiles(Profiles.of("prod", "production"));
                if (production && (environment.getProperty("springdoc.api-docs.enabled", Boolean.class, true)
                                || environment.getProperty("springdoc.swagger-ui.enabled", Boolean.class, true))) {
                        throw new IllegalArgumentException("Swagger/OpenAPI must be disabled in production");
                }
                this.allowedOrigins = CorsAllowedOrigins.resolve(configuredAllowedOrigins, production);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtFilter jwtFilter,
                        AuthRateLimiter rateLimiter, HandlerMappingIntrospector introspector) throws Exception {
                var mvc = new MvcRequestMatcher.Builder(introspector);
                var login = mvc.pattern(HttpMethod.POST, "/auth/login");
                var registration = mvc.pattern(HttpMethod.POST, "/users");
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/webjars/swagger-ui/**", "/swagger-resources/**",
                                                                "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml")
                                                .access((authentication, context) ->
                                                        new org.springframework.security.authorization.AuthorizationDecision(!production))
                                                .requestMatchers(registration, login).permitAll()
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint((request, response, exception) ->
                                                                response.sendError(
                                                                                HttpServletResponse.SC_UNAUTHORIZED,
                                                                                "Unauthorized")))
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(new AuthRateLimitFilter(rateLimiter, login, registration), JwtFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(allowedOrigins);
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key"));
                configuration.setExposedHeaders(List.of("Retry-After"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
