package com.myfitness.api.practice.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.myfitness.api.practice.dto.MemoRequestDto;
import com.myfitness.api.practice.dto.MemoResponseDto;
import com.myfitness.api.practice.entity.Memo;
import com.myfitness.api.practice.mapper.MemoMapper;
import com.myfitness.api.practice.repository.MemoRepository;

@Service
public class MemoService {

    private final MemoRepository memoRepository;
    private final MemoMapper memoMapper;

    public MemoService(MemoRepository memoRepository, MemoMapper memoMapper) {
        this.memoRepository = memoRepository;
        this.memoMapper = memoMapper;
    }

    public List<MemoResponseDto> getAllMemos() {
        return memoRepository.findAll()
                .stream()
                .map(memoMapper::toResponseDto)
                .toList();
    }

    public MemoResponseDto getMemoById(Long id) {
        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo not found: id=" + id));

        return memoMapper.toResponseDto(memo);
    }

    public MemoResponseDto createMemo(MemoRequestDto req) {
        Memo memo = memoMapper.toEntity(req);
        Memo saved = memoRepository.save(memo);
        return memoMapper.toResponseDto(saved);
    }

    public MemoResponseDto updateMemo(Long id, MemoRequestDto req) {

        Memo memo = memoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo not found: id=" + id));

        // mapper を使って更新
        memoMapper.toEntity(memo, req);

        Memo saved = memoRepository.save(memo);
        return memoMapper.toResponseDto(saved);
    }

    public void deleteMemo(Long id) {
        if (!memoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Memo not found: id=" + id);
        }
        memoRepository.deleteById(id);
    }
}
