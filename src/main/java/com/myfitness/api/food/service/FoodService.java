package com.myfitness.api.food.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.myfitness.api.food.dto.CreateFoodRequest;
import com.myfitness.api.food.dto.FoodResponse;
import com.myfitness.api.food.entity.Food;
import com.myfitness.api.food.repository.FoodRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FoodService {

    private final FoodRepository foodRepository;

    @Transactional
    public FoodResponse create(CreateFoodRequest request) {
        Food food = new Food(
                request.getName(),
                request.getCaloriesPer100g(),
                request.getProteinPer100g(),
                request.getFatPer100g(),
                request.getCarbPer100g());

        Food saved = foodRepository.save(food);
        return FoodResponse.from(saved);
    }

    public List<FoodResponse> findAll() {
        return foodRepository.findAll()
                .stream()
                .map(FoodResponse::from)
                .toList();
    }

    public FoodResponse findById(Long foodId) {
        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Food not found"));

        return FoodResponse.from(food);
    }
}