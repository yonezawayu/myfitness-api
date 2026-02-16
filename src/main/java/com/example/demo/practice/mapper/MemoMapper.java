package com.example.demo.practice.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.practice.dto.MemoRequestDto;
import com.example.demo.practice.dto.MemoResponseDto;
import com.example.demo.practice.entity.Memo;

@Component
public class MemoMapper {

    public Memo toEntity(MemoRequestDto dto) {
        Memo memo = new Memo();
        memo.setTitle(dto.getTitle());
        return memo;
    }

    public Memo toEntity(Memo memo, MemoRequestDto dto) {
        memo.setTitle(dto.getTitle());
        return memo;
    }

    public MemoResponseDto toResponseDto(Memo memo) {
        return new MemoResponseDto(
                memo.getId(),
                memo.getTitle());
    }
}
