package com.example.demo.practice.dto;

public class MemoResponseDto {

    private Long id;
    private String title;

    public MemoResponseDto() {
    }

    public MemoResponseDto(Long id, String title) {
        this.id = id;
        this.title = title;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
