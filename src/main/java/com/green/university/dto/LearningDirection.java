package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 학습 방향 제시
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningDirection {
    // 강점 분석
    private String strengths;

    // 약점 분석
    private String weaknesses;

    // 개선 방향 제안
    private List<String> improvementSuggestions;

    // 학습 전략
    private List<String> learningStrategies;

    // 주의 사항
    private List<String> cautions;

    // 추천 학습 패턴
    private String recommendedPattern;
}