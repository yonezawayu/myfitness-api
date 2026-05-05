package com.myfitness.api.meal.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.meal.dto.MealLogRequestDto;
import com.myfitness.api.meal.dto.MealLogResponseDto;
import com.myfitness.api.meal.entity.MealLog;
import com.myfitness.api.meal.mapper.MealLogMapper;
import com.myfitness.api.meal.repository.MealItemRepository;
import com.myfitness.api.meal.repository.MealLogRepository;

@Service
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final MealLogMapper mealLogMapper;
    private final MealItemRepository mealItemRepository;

    public MealLogService(
            MealLogRepository mealLogRepository,
            MealLogMapper mealLogMapper,
            MealItemRepository mealItemRepository) {
        this.mealLogRepository = mealLogRepository;
        this.mealLogMapper = mealLogMapper;
        this.mealItemRepository = mealItemRepository;
    }

    public List<MealLogResponseDto> getAll() {
        return mealLogRepository.findAll()
                .stream()
                .map(this::toResponseDtoWithSummary)
                .collect(Collectors.toList());
    }

    public MealLogResponseDto getById(Long id) {
        MealLog log = mealLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "MealLog not found: id=" + id));

        return toResponseDtoWithSummary(log);
    }

    public MealLogResponseDto create(MealLogRequestDto req) {
        MealLog log = mealLogMapper.fromRequestDto(req);
        MealLog saved = mealLogRepository.save(log);

        return toResponseDtoWithSummary(saved);
    }

    public MealLogResponseDto update(Long id, MealLogRequestDto req) {
        MealLog existing = mealLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "MealLog not found: id=" + id));

        mealLogMapper.updateEntity(existing, req);

        MealLog saved = mealLogRepository.save(existing);
        return toResponseDtoWithSummary(saved);
    }

    public void delete(Long id) {
        if (!mealLogRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "MealLog not found: id=" + id);
        }
        mealLogRepository.deleteById(id);
    }

    private MealLogResponseDto toResponseDtoWithSummary(MealLog log) {
        BigDecimal totalCalories = mealItemRepository.sumCaloriesByMealLogId(log.getId());
        BigDecimal totalProtein = mealItemRepository.sumProteinByMealLogId(log.getId());

        return new MealLogResponseDto(
                log.getId(),
                log.getMealName(),
                log.getDate(),
                log.getMemo(),
                totalCalories,
                totalProtein);
    }
}