package com.example.demo.record.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.record.dto.RecordRequestDto;
import com.example.demo.record.dto.RecordResponseDto;
import com.example.demo.record.service.RecordService;

@RestController
@RequestMapping("/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ResponseEntity<List<RecordResponseDto>> getAllRecords() {
        return ResponseEntity.ok(recordService.getAllRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecordResponseDto> getRecord(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @PostMapping("/add")
    public ResponseEntity<RecordResponseDto> createRecord(
            @Valid @RequestBody RecordRequestDto request) {
        return ResponseEntity.ok(recordService.createRecord(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecordResponseDto> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody RecordRequestDto request) {
        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
