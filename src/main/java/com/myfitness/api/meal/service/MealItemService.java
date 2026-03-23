package com.myfitness.api.meal.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.food.entity.Food;
import com.myfitness.api.food.repository.FoodRepository;
import com.myfitness.api.meal.dto.CreateMealItemRequest;
import com.myfitness.api.meal.dto.MealItemResponse;
import com.myfitness.api.meal.entity.MealItem;
import com.myfitness.api.meal.repository.MealItemRepository;
import com.myfitness.api.meal.repository.MealLogRepository;

@Service
public class MealItemService {

    private final MealItemRepository mealItemRepository;
    private final MealLogRepository mealLogRepository;
    private final FoodRepository foodRepository;

    public MealItemService(
            MealItemRepository mealItemRepository,
            MealLogRepository mealLogRepository,
            FoodRepository foodRepository) {
        this.mealItemRepository = mealItemRepository;
        this.mealLogRepository = mealLogRepository;
        this.foodRepository = foodRepository;
    }

    public MealItemResponse create(CreateMealItemRequest request) {
        if (!mealLogRepository.existsById(request.getMealLogId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "MealLog not found: id=" + request.getMealLogId());
        }

        Food food = foodRepository.findById(request.getFoodId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Food not found: id=" + request.getFoodId()));

        BigDecimal ratio = request.getQuantityG()
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);

        BigDecimal calories = food.getCaloriesPer100g()
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal protein = food.getProteinPer100g()
                .multiply(ratio)
                .setScale(2, RoundingMode.HALF_UP);

        MealItem item = new MealItem(
                request.getMealLogId(),
                request.getFoodId(),
                request.getQuantityG().setScale(2, RoundingMode.HALF_UP),
                calories,
                protein);

        MealItem saved = mealItemRepository.save(item);

        return toResponse(saved);
    }

    public List<MealItemResponse> getByMealLogId(Long mealLogId) {
        return mealItemRepository.findByMealLogId(mealLogId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MealItemResponse toResponse(MealItem item) {
        return new MealItemResponse(
                item.getId(),
                item.getMealLogId(),
                item.getFoodId(),
                item.getQuantityG(),
                item.getCalories(),
                item.getProtein());
    }
}