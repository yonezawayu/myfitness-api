package com.example.demo.record.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.record.entity.Record;

public interface RecordRepository extends JpaRepository<Record, Long> {

    Optional<Record> findByDate(LocalDate date);

    Optional<Record> findTopByDateOrderByIdDesc(LocalDate date);

}
