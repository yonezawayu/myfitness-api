package com.myfitness.api.meal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.myfitness.api.meal.dto.CreateMealItemRequest;
import com.myfitness.api.meal.dto.MealItemResponse;
import com.myfitness.api.meal.service.MealItemService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "MealItem", description = "食事アイテム")
@RestController
@RequestMapping("/meal-items")
public class MealItemController {

    private final MealItemService mealItemService;

    public MealItemController(MealItemService mealItemService) {
        this.mealItemService = mealItemService;
    }

    @PostMapping
    public ResponseEntity<MealItemResponse> create(
            @Valid @RequestBody CreateMealItemRequest request) {
        MealItemResponse created = mealItemService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/meal-log/{mealLogId}")
    public ResponseEntity<List<MealItemResponse>> getByMealLogId(@PathVariable Long mealLogId) {
        return ResponseEntity.ok(mealItemService.getByMealLogId(mealLogId));
    }
}