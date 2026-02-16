package com.example.demo.training.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.training.dto.TrainingLogRequestDto;
import com.example.demo.training.dto.TrainingLogResponseDto;
import com.example.demo.training.entity.TrainingLog;

@Component
public class TrainingLogMapper {

    // RequestDto → Entity（新規作成用）
    public TrainingLog fromRequestDto(TrainingLogRequestDto req) {
        TrainingLog log = new TrainingLog();
        log.setTrainingName(req.getTrainingName());
        log.setCaloriesBurned(req.getCaloriesBurned());
        log.setDate(req.getDate());
        return log;
    }

    // Entity → ResponseDto
    public TrainingLogResponseDto toResponseDto(TrainingLog log) {
        return new TrainingLogResponseDto(
                log.getId(),
                log.getTrainingName(),
                log.getCaloriesBurned(),
                log.getDate());
    }

    // Entity 更新（PUT 用）
    public void updateEntity(TrainingLog entity, TrainingLogRequestDto req) {
        entity.setTrainingName(req.getTrainingName());
        entity.setCaloriesBurned(req.getCaloriesBurned());
        entity.setDate(req.getDate());
    }
}
