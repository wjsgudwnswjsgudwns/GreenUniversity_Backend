package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 맞춤형 학습 지원 데이터 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalizedLearningDTO {
    // 학생 기본 정보
    private Integer studentId;
    private String studentName;
    private String departmentName;
    private Integer currentGrade;
    private Integer currentSemester;

    // 학습 이력 분석
    private LearningHistoryAnalysis learningHistory;

    // 추천 과목
    private List<RecommendedSubject> recommendedMajors;
    private List<RecommendedSubject> recommendedElectives;

    // 학습 방향
    private LearningDirection learningDirection;

    // 졸업 요건
    private GraduationRequirement graduationRequirement;

    // AI 종합 분석 코멘트
    private String aiAnalysisComment;

    // 분석 생성 시간 (추가)
    private LocalDateTime analysisDate;
}