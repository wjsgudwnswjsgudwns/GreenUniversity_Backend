package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 학습 이력 분석 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningHistoryAnalysis {
    // 전체 평점
    private Double overallGPA;

    // 전공 평점
    private Double majorGPA;

    // 교양 평점
    private Double electiveGPA;

    // 최근 학기 평점 (최근 2학기)
    private Double recentGPA;

    // 총 이수 학점
    private Integer totalCredits;

    // 전공 이수 학점
    private Integer majorCredits;

    // 교양 이수 학점
    private Integer electiveCredits;

    // 성적 추이 (상승/유지/하락)
    private String gradeTrend;

    // 출석률
    private Double attendanceRate;

    // 과제 제출률
    private Double homeworkRate;

    // 강점 과목 분야
    private List<String> strongAreas;

    // 약점 과목 분야
    private List<String> weakAreas;
}
