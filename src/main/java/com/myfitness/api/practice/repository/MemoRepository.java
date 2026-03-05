package com.myfitness.api.practice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.myfitness.api.practice.entity.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}
