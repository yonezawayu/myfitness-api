package com.myfitness.api.training.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.myfitness.api.training.dto.TrainingLogRequestDto;
import com.myfitness.api.training.dto.TrainingLogResponseDto;
import com.myfitness.api.training.service.TrainingLogService;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "TrainingLog", description = "トレーニングログ（消費カロリー）")
@RestController
@RequestMapping("/training-logs")
public class TrainingLogController {

    private final TrainingLogService trainingLogService;

    public TrainingLogController(TrainingLogService trainingLogService) {
        this.trainingLogService = trainingLogService;
    }

    @GetMapping
    public ResponseEntity<List<TrainingLogResponseDto>> getAll() {
        return ResponseEntity.ok(trainingLogService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrainingLogResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(trainingLogService.getById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<TrainingLogResponseDto> create(
            @RequestBody TrainingLogRequestDto request) {
        return ResponseEntity.ok(trainingLogService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrainingLogResponseDto> update(
            @PathVariable Long id,
            @RequestBody TrainingLogRequestDto request) {
        return ResponseEntity.ok(trainingLogService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trainingLogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
