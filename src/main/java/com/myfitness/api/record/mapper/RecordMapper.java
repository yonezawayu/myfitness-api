package com.myfitness.api.record.mapper;

import com.myfitness.api.record.dto.RecordRequestDto;
import com.myfitness.api.record.dto.RecordResponseDto;
import com.myfitness.api.record.entity.Record;

public class RecordMapper {

    // DTO → Entity
    public static Record toEntity(RecordRequestDto dto) {
        Record r = new Record();
        r.setWeight(dto.getWeight());
        r.setCalories(dto.getCalories());
        r.setDate(dto.getDate());
        return r;
    }

    // Entity → DTO
    public static RecordResponseDto toDto(Record entity) {
        return new RecordResponseDto(
                entity.getId(),
                entity.getWeight(),
                entity.getCalories(),
                entity.getDate());
    }

    // Update 用（Entity に DTO の値を反映）
    public static void updateEntity(Record entity, RecordRequestDto dto) {
        entity.setWeight(dto.getWeight());
        entity.setCalories(dto.getCalories());
        entity.setDate(dto.getDate());
    }
}
