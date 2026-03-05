package com.myfitness.api.training.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.training.dto.TrainingLogRequestDto;
import com.myfitness.api.training.dto.TrainingLogResponseDto;
import com.myfitness.api.training.entity.TrainingLog;
import com.myfitness.api.training.mapper.TrainingLogMapper;
import com.myfitness.api.training.repository.TrainingLogRepository;

@Service
public class TrainingLogService {

    private final TrainingLogRepository trainingLogRepository;
    private final TrainingLogMapper trainingLogMapper;

    public TrainingLogService(
            TrainingLogRepository trainingLogRepository,
            TrainingLogMapper trainingLogMapper) {
        this.trainingLogRepository = trainingLogRepository;
        this.trainingLogMapper = trainingLogMapper;
    }

    // 全件取得
    public List<TrainingLogResponseDto> getAll() {
        return trainingLogRepository.findAll()
                .stream()
                .map(trainingLogMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    // 1件取得
    public TrainingLogResponseDto getById(Long id) {
        TrainingLog log = trainingLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "TrainingLog not found: id = " + id));

        return trainingLogMapper.toResponseDto(log);
    }

    // 作成
    public TrainingLogResponseDto create(TrainingLogRequestDto req) {
        TrainingLog log = trainingLogMapper.fromRequestDto(req);
        TrainingLog saved = trainingLogRepository.save(log);
        return trainingLogMapper.toResponseDto(saved);
    }

    // 更新
    public TrainingLogResponseDto update(Long id, TrainingLogRequestDto req) {

        TrainingLog existing = trainingLogRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "TrainingLog not found: id = " + id));

        trainingLogMapper.updateEntity(existing, req);

        TrainingLog saved = trainingLogRepository.save(existing);

        return trainingLogMapper.toResponseDto(saved);
    }

    // 削除
    public void delete(Long id) {
        if (!trainingLogRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "TrainingLog not found: id = " + id);
        }
        trainingLogRepository.deleteById(id);
    }
}
