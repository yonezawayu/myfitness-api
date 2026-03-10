package com.myfitness.api.user.dto;

public record UserResponse(
        Long id,
        String email,
        String role) {
}