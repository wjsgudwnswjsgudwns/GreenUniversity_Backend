package com.green.university.repository;

import com.green.university.repository.model.RecommendedSubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 추천 과목 Repository
 */
public interface RecommendedSubjectJpaRepository extends JpaRepository<RecommendedSubjectEntity, Integer> {

    /**
     * 분석 ID로 추천 과목 조회
     */
    List<RecommendedSubjectEntity> findByAnalysisIdOrderByDisplayOrder(Integer analysisId);

    /**
     * 분석 ID와 추천 타입으로 조회
     */
    List<RecommendedSubjectEntity> findByAnalysisIdAndRecommendationTypeOrderByDisplayOrder(
            Integer analysisId, String recommendationType);

    /**
     * 분석 ID로 추천 과목 삭제
     */
    void deleteByAnalysisId(Integer analysisId);

    /**
     * 과목 정보를 포함한 추천 과목 조회
     */
    @Query("SELECT r FROM RecommendedSubjectEntity r " +
            "JOIN FETCH r.subject s " +
            "JOIN FETCH s.professor " +
            "WHERE r.analysisId = :analysisId " +
            "ORDER BY r.displayOrder")
    List<RecommendedSubjectEntity> findByAnalysisIdWithSubjectDetails(@Param("analysisId") Integer analysisId);
}