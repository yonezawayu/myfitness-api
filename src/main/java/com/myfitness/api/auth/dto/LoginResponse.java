package com.myfitness.api.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds) {

    @Override
    public String toString() {
        return "LoginResponse[accessToken=[REDACTED], tokenType=" + tokenType
                + ", expiresInSeconds=" + expiresInSeconds + "]";
    }
}
