package com.example.demo.meal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.meal.dto.MealLogRequestDto;
import com.example.demo.meal.dto.MealLogResponseDto;
import com.example.demo.meal.service.MealLogService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "MealLog", description = "食事ログ")
@RestController
@RequestMapping("/meal-logs")
public class MealLogController {

    private final MealLogService mealLogService;

    public MealLogController(MealLogService mealLogService) {
        this.mealLogService = mealLogService;
    }

    // 全件取得
    @GetMapping
    public ResponseEntity<List<MealLogResponseDto>> getAll() {
        return ResponseEntity.ok(mealLogService.getAll());
    }

    // 詳細取得
    @GetMapping("/{id}")
    public ResponseEntity<MealLogResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mealLogService.getById(id));
    }

    // 新規作成
    @PostMapping("/add")
    public ResponseEntity<MealLogResponseDto> create(
            @Valid @RequestBody MealLogRequestDto request) {
        MealLogResponseDto created = mealLogService.create(request);
        return ResponseEntity.ok(created);
    }

    // 更新
    @PutMapping("/{id}")
    public ResponseEntity<MealLogResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody MealLogRequestDto request) {
        return ResponseEntity.ok(mealLogService.update(id, request));
    }

    // 削除
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mealLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
