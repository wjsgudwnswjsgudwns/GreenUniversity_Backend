package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 졸업 요건 분석
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationRequirement {
    // 졸업 필요 총 학점
    private Integer totalRequiredCredits;

    // 현재 이수 학점
    private Integer currentCredits;

    // 남은 학점
    private Integer remainingCredits;

    // 전공 필수 필요 학점
    private Integer majorRequiredCredits;

    // 전공 필수 이수 학점
    private Integer majorCompletedCredits;

    // 전공 남은 학점
    private Integer majorRemainingCredits;

    // 교양 필요 학점
    private Integer electiveRequiredCredits;

    // 교양 이수 학점
    private Integer electiveCompletedCredits;

    // 교양 남은 학점
    private Integer electiveRemainingCredits;

    // 졸업 가능 여부
    private Boolean canGraduate;

    // 졸업까지 남은 학기 (예상)
    private Integer semestersToGraduation;

    // 학기당 권장 이수 학점
    private Integer recommendedCreditsPerSemester;
}