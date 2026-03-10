package com.myfitness.api.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {
}