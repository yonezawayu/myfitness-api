package com.myfitness.api.food.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.myfitness.api.food.dto.CreateFoodRequest;
import com.myfitness.api.food.dto.FoodResponse;
import com.myfitness.api.food.service.FoodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FoodResponse create(@Validated @RequestBody CreateFoodRequest request) {
        return foodService.create(request);
    }

    @GetMapping
    public List<FoodResponse> findAll() {
        return foodService.findAll();
    }

    @GetMapping("/{foodId}")
    public FoodResponse findById(@PathVariable Long foodId) {
        return foodService.findById(foodId);
    }
}