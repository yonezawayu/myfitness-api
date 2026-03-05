package com.myfitness.api.practice.dto;

import jakarta.validation.constraints.NotBlank;

public class MemoRequestDto {

    @NotBlank(message = "title is required")
    private String title;

    public MemoRequestDto() {
    }

    public MemoRequestDto(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
