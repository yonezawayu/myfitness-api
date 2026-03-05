package com.example.demo.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 今はAPI前提なのでCSRFはOFF（ブラウザフォーム運用しない前提）
                                .csrf(csrf -> csrf.disable())

                                // セッションは使わない（APIとしての基本形）
                                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 認可ルール
                                .authorizeHttpRequests(auth -> auth
                                                // Swagger / OpenAPI は見れるように（ここは好みで要認証でもOK）
                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                // それ以外は認証必須
                                                .anyRequest().authenticated())

                                // まずはBasic認証でOK（JWTは次でやる）
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }
}