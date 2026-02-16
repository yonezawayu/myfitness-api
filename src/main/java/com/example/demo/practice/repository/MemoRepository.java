package com.example.demo.practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.practice.entity.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}
