package com.myfitness.api.record.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.myfitness.api.record.entity.Record;

public interface RecordRepository extends JpaRepository<Record, Long> {

    Optional<Record> findByDate(LocalDate date);

    Optional<Record> findTopByDateOrderByIdDesc(LocalDate date);

}
