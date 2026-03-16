package com.myfitness.api.meal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.myfitness.api.meal.dto.MealLogRequestDto;
import com.myfitness.api.meal.dto.MealLogResponseDto;
import com.myfitness.api.meal.service.MealLogService;

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

    @GetMapping
    public ResponseEntity<List<MealLogResponseDto>> getAll() {
        return ResponseEntity.ok(mealLogService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MealLogResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mealLogService.getById(id));
    }

    @PostMapping
    public ResponseEntity<MealLogResponseDto> create(
            @Valid @RequestBody MealLogRequestDto request) {
        MealLogResponseDto created = mealLogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MealLogResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody MealLogRequestDto request) {
        return ResponseEntity.ok(mealLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        mealLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}