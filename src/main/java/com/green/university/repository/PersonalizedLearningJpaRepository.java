package com.green.university.repository;

import com.green.university.repository.model.PersonalizedLearningAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 맞춤형 학습 분석 Repository
 */
public interface PersonalizedLearningJpaRepository extends JpaRepository<PersonalizedLearningAnalysis, Integer> {

    /**
     * 학생의 최신 분석 결과 조회
     */
    Optional<PersonalizedLearningAnalysis> findByStudentIdAndIsLatestTrue(Integer studentId);

    /**
     * 학생의 특정 학기 분석 결과 조회
     */
    Optional<PersonalizedLearningAnalysis> findByStudentIdAndAnalysisYearAndAnalysisSemester(
            Integer studentId, Integer year, Integer semester);

    /**
     * 학생의 전체 분석 이력 조회 (최신순)
     */
    List<PersonalizedLearningAnalysis> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    /**
     * 기존 최신 분석을 최신이 아니도록 업데이트
     */
    @Modifying
    @Query("UPDATE PersonalizedLearningAnalysis p SET p.isLatest = false WHERE p.studentId = :studentId AND p.isLatest = true")
    void updatePreviousLatestToFalse(@Param("studentId") Integer studentId);

    /**
     * 분석 ID로 조회
     */
    Optional<PersonalizedLearningAnalysis> findById(Integer id);
}