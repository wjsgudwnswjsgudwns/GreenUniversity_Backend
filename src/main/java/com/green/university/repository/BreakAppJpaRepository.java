package com.green.university.repository;

import com.green.university.dto.BreakAppFormDto;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.BreakApp;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link BreakApp} entities.
 */
public interface BreakAppJpaRepository extends JpaRepository<BreakApp, Integer> {

    // 휴학 신청하기
    // breakAppJpaRepository.save

    // 특정 휴학 신청서 조회하기
    // breakAppJpaRepository.findById

    // 휴학 신청 취소하기 (학생용)
    // breakAppJpaRepository.deleteById

    // 휴학 신청 처리하기 (교직원용)
    // Service에서 findById()로 조회 후 entity.setStatus() 하고 save() 사용

    // 학생의 휴학 신청 조회하기
    public List<BreakApp> findByStudentId(Integer studentId);

    // 처리되지 않은 휴학 신청 조회하기 (교직원용)
    public List<BreakApp> findByStatus(String status);
}