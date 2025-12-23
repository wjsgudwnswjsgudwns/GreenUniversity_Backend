package com.green.university.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StuSubResponseDto {

    private Integer studentId;
    private String studentName;
    private String deptName;

    private Integer absent;
    private Integer lateness;
    private Integer homework;
    private Integer midExam;
    private Integer finalExam;
    private Integer convertedMark;
    // 계산된 환산 점수 (Double로 저장하여 소수점 지원)
    private Double computedMark;
    // 그룹 분류 (A: 상위 30%, B: 중간 40%, C: 하위 30%, F: 결석 기준)
    private String group;
    // 추천 등급
    private String recommendedGrade;
    // 현재 등급
    private String currentGrade;
}