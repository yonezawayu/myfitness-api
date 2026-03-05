package com.myfitness.api.meal.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.meal.dto.MealLogRequestDto;
import com.myfitness.api.meal.dto.MealLogResponseDto;
import com.myfitness.api.meal.entity.MealLog;
import com.myfitness.api.meal.mapper.MealLogMapper;
import com.myfitness.api.meal.repository.MealLogRepository;

@Service
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final MealLogMapper mealLogMapper;

    public MealLogService(MealLogRepository mealLogRepository, MealLogMapper mealLogMapper) {
        this.mealLogRepository = mealLogRepository;
        this.mealLogMapper = mealLogMapper;
    }

    // 全件取得
    public List<MealLogResponseDto> getAll() {
        return mealLogRepository.findAll()
                .stream()
                .map(mealLogMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // 詳細取得
    public MealLogResponseDto getById(Long id) {
        MealLog log = mealLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "MealLog not found: id=" + id));

        return mealLogMapper.toResponseDto(log);
    }

    // 新規作成
    public MealLogResponseDto create(MealLogRequestDto req) {
        MealLog log = mealLogMapper.fromRequestDto(req);
        MealLog saved = mealLogRepository.save(log);
        return mealLogMapper.toResponseDto(saved);
    }

    // 更新
    public MealLogResponseDto update(Long id, MealLogRequestDto req) {
        MealLog existing = mealLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "MealLog not found: id=" + id));

        mealLogMapper.updateEntity(existing, req);

        MealLog saved = mealLogRepository.save(existing);
        return mealLogMapper.toResponseDto(saved);
    }

    // 削除
    public void delete(Long id) {
        if (!mealLogRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "MealLog not found: id=" + id);
        }
        mealLogRepository.deleteById(id);
    }
}
