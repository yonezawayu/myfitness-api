package com.myfitness.api.record.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.myfitness.api.record.dto.RecordRequestDto;
import com.myfitness.api.record.dto.RecordResponseDto;
import com.myfitness.api.record.entity.Record;
import com.myfitness.api.record.mapper.RecordMapper;
import com.myfitness.api.record.repository.RecordRepository;

@Service
public class RecordService {

    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    // 全件取得
    public List<RecordResponseDto> getAllRecords() {
        return recordRepository.findAll()
                .stream()
                .map(RecordMapper::toDto)
                .toList();
    }

    // 1件取得
    public RecordResponseDto getRecordById(Long id) {
        Record r = recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found id=" + id));

        return RecordMapper.toDto(r);
    }

    // 作成
    public RecordResponseDto createRecord(RecordRequestDto req) {
        Record newRecord = RecordMapper.toEntity(req);
        Record saved = recordRepository.save(newRecord);
        return RecordMapper.toDto(saved);
    }

    // 更新
    public RecordResponseDto updateRecord(Long id, RecordRequestDto req) {
        Record existing = recordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found id=" + id));

        RecordMapper.updateEntity(existing, req);
        Record saved = recordRepository.save(existing);

        return RecordMapper.toDto(saved);
    }

    // 削除
    public void deleteRecord(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new RuntimeException("Record not found id=" + id);
        }
        recordRepository.deleteById(id);
    }
}
