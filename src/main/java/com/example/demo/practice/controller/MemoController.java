package com.example.demo.practice.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.practice.dto.MemoRequestDto;
import com.example.demo.practice.dto.MemoResponseDto;
import com.example.demo.practice.service.MemoService;

@RestController
@RequestMapping("/practice/memos")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping
    public ResponseEntity<List<MemoResponseDto>> getAllMemos() {
        return ResponseEntity.ok(memoService.getAllMemos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MemoResponseDto> getMemo(@PathVariable Long id) {
        return ResponseEntity.ok(memoService.getMemoById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<MemoResponseDto> createMemo(
            @Valid @RequestBody MemoRequestDto request) {

        MemoResponseDto created = memoService.createMemo(request);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoResponseDto> updateMemo(
            @PathVariable Long id,
            @Valid @RequestBody MemoRequestDto request) {

        MemoResponseDto updated = memoService.updateMemo(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMemo(@PathVariable Long id) {
        memoService.deleteMemo(id);
        return ResponseEntity.noContent().build();
    }
}
