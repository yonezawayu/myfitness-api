package com.myfitness.api.training.mapper;

import org.springframework.stereotype.Component;

import com.myfitness.api.training.dto.TrainingLogRequestDto;
import com.myfitness.api.training.dto.TrainingLogResponseDto;
import com.myfitness.api.training.entity.TrainingLog;

@Component
public class TrainingLogMapper {

    // RequestDto → Entity（新規作成用）
    public TrainingLog fromRequestDto(TrainingLogRequestDto req) {
        TrainingLog log = new TrainingLog();
        log.setTrainingName(req.getTrainingName());
        log.setDate(req.getDate());
        return log;
    }

    // Entity → ResponseDto
    public TrainingLogResponseDto toResponseDto(TrainingLog log) {
        return new TrainingLogResponseDto(
                log.getId(),
                log.getTrainingName(),
                log.getDate());
    }

    // Entity 更新（PUT 用）
    public void updateEntity(TrainingLog entity, TrainingLogRequestDto req) {
        entity.setTrainingName(req.getTrainingName());
        entity.setDate(req.getDate());
    }
}
