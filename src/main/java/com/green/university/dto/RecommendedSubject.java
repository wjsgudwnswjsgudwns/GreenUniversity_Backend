package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 추천 과목 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedSubject {
    private Integer subjectId;
    private String subjectName;
    private String professorName;
    private Integer credits;
    private String subjectType; // 전공필수, 전공선택, 교양필수, 교양선택
    private Double recommendScore; // 추천 점수 (0~100)
    private String recommendReason; // 추천 이유
    private List<String> relatedSubjects; // 연관 과목
    private Double averageGrade; // 평균 평점
    private Integer capacity; // 정원
    private Integer currentStudents; // 현재 수강 인원
}