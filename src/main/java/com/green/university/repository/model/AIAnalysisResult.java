package com.green.university.repository.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * AI가 분석한 학생별 항목별 결과를 저장하는 엔티티
 */
@Data
@Entity
@Table(name = "ai_analysis_result_tb")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AIAnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 분석 대상 학생
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    /**
     * 분석 대상 과목
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Subject subject;

    @Column(name = "subject_id", insertable = false, updatable = false)
    private Integer subjectId;

    /**
     * 분석 연도
     */
    @Column(name = "analysis_year", nullable = false)
    private Integer analysisYear;

    /**
     * 분석 학기
     */
    @Column(name = "semester", nullable = false)
    private Integer semester;

    /**
     * 출결 상태: NORMAL(정상), CAUTION(주의), RISK(위험), CRITICAL(심각)
     */
    @Column(name = "attendance_status", nullable = false, length = 20)
    private String attendanceStatus;

    /**
     * 과제 상태
     */
    @Column(name = "homework_status", nullable = false, length = 20)
    private String homeworkStatus;

    /**
     * 중간고사 상태
     */
    @Column(name = "midterm_status", nullable = false, length = 20)
    private String midtermStatus;

    /**
     * 기말고사 상태
     */
    @Column(name = "final_status", nullable = false, length = 20)
    private String finalStatus;

    /**
     * 등록금 상태
     */
    @Column(name = "tuition_status", nullable = false, length = 20)
    private String tuitionStatus;

    /**
     * 상담 상태 (상담 내용 AI 분석 결과)
     */
    @Column(name = "counseling_status", length = 20)
    private String counselingStatus;

    /**
     * 종합 위험도: NORMAL(정상), CAUTION(주의), RISK(위험), CRITICAL(심각)
     */
    @Column(name = "overall_risk", nullable = false, length = 20)
    private String overallRisk;

    /**
     * AI 분석 상세 내용
     */
    @Column(name = "analysis_detail", columnDefinition = "TEXT")
    private String analysisDetail;

    /**
     * 분석 일시
     */
    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        analyzedAt = LocalDateTime.now();
    }
}