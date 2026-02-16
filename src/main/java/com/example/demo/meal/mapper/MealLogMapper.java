package com.example.demo.meal.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.meal.dto.MealLogRequestDto;
import com.example.demo.meal.dto.MealLogResponseDto;
import com.example.demo.meal.entity.MealLog;

@Component
public class MealLogMapper {

    // RequestDto → Entity（新規作成用）
    public MealLog fromRequestDto(MealLogRequestDto dto) {
        MealLog entity = new MealLog();

        entity.setMealName(dto.getMealName());
        entity.setCalories(dto.getCalories());
        entity.setDate(dto.getDate());
        entity.setMemo(dto.getMemo());
        entity.setProtein(dto.getProtein());

        return entity;
    }

    // Entity → ResponseDto
    public MealLogResponseDto toResponseDto(MealLog entity) {
        return new MealLogResponseDto(
                entity.getId(),
                entity.getMealName(),
                entity.getCalories(),
                entity.getDate(),
                entity.getMemo(),
                entity.getProtein());
    }

    // Entity 更新用（PUT）
    public void updateEntity(MealLog entity, MealLogRequestDto dto) {
        entity.setMealName(dto.getMealName());
        entity.setCalories(dto.getCalories());
        entity.setDate(dto.getDate());
        entity.setMemo(dto.getMemo());
        entity.setProtein(dto.getProtein());
    }
}
