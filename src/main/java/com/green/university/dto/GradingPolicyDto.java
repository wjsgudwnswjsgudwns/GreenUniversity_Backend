package com.green.university.dto;

import lombok.Data;

/**
 * 성적 가중치 정책 DTO
 * @author 김지현
 */
@Data
public class GradingPolicyDto {
    
    // 출결 가중치 (%)
    private Integer attendanceWeight;
    // 과제 가중치 (%)
    private Integer homeworkWeight;
    // 중간고사 가중치 (%)
    private Integer midtermWeight;
    // 기말고사 가중치 (%)
    private Integer finalWeight;
    
    // 출결 만점
    private Integer attendanceMax;
    // 과제 만점
    private Integer homeworkMax;
    // 중간고사 만점
    private Integer midtermMax;
    // 기말고사 만점
    private Integer finalMax;
    
    // 지각 n회 = 1결석
    private Integer latenessPerAbsent;
    // 지각 감점 (회당)
    private Integer latenessPenaltyPer;
    // 지각 무감점 횟수
    private Integer latenessFreeCount;
    
    // 기본값 생성자
    public GradingPolicyDto() {
        this.attendanceWeight = 10;
        this.homeworkWeight = 30;
        this.midtermWeight = 30;
        this.finalWeight = 30;
        this.attendanceMax = 100;
        this.homeworkMax = 100;
        this.midtermMax = 100;
        this.finalMax = 100;
        this.latenessPerAbsent = 3;
        this.latenessPenaltyPer = 0;
        this.latenessFreeCount = 0;
    }
}

