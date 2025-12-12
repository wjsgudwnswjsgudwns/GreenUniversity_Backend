package com.green.university.repository.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 상담 일정 및 내용을 나타내는 JPA 엔티티
 */
@Data
@Entity
@Table(name = "ai_counseling_tb")
public class AICounseling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 상담 대상 학생
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    /**
     * 상담 진행 교수
     */
    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    @Column(name = "professor_id", insertable = false, updatable = false)
    private Integer professorId;

    /**
     * 상담 관련 과목
     */
    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "subject_id", insertable = false, updatable = false)
    private Integer subjectId;

    /**
     * 상담 예정 일시
     */
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    /**
     * 상담 완료 여부
     */
    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    /**
     * 교수가 작성한 상담 내용 (AI 분석 대상)
     */
    @Column(name = "counseling_content", columnDefinition = "TEXT")
    private String counselingContent;

    /**
     * 상담 완료 일시
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Column(name = "ai_analysis_result", length = 20)
    private String aiAnalysisResult;
}