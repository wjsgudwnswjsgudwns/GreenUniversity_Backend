package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Tuition;
import com.green.university.repository.model.TuitionId;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Tuition} entities.
 */
public interface TuitionJpaRepository extends JpaRepository<Tuition, TuitionId> {
    // 학생 전체 납부 내역 (최신 학기 순)
    List<Tuition> findByIdStudentIdOrderByIdTuiYearDescIdSemesterDesc(Integer studentId);

    // 상태별 납부 내역
    List<Tuition> findByIdStudentIdAndStatusOrderByIdTuiYearDescIdSemesterDesc(Integer studentId, Boolean status);

    // 특정 학기 고지서
    Optional<Tuition> findByIdStudentIdAndIdTuiYearAndIdSemester(Integer studentId,
                                                                 Integer tuiYear,
                                                                 Integer semester);

}