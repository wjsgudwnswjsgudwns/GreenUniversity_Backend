package com.green.university.repository.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 중도 이탈 위험 알림을 저장하는 엔티티
 * AI가 학생의 중도 이탈 가능성이 높아졌다고 판단할 때 생성
 */
@Data
@Entity
@Table(name = "ai_risk_alert_tb")
public class AIRiskAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 위험 감지 대상 학생
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    /**
     * 관련 과목 (특정 과목에서 위험 감지된 경우)
     */
    @ManyToOne
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(name = "subject_id", insertable = false, updatable = false)
    private Integer subjectId;

    /**
     * 위험도: CAUTION(주의), RISK(위험), CRITICAL(심각)
     */
    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    /**
     * 위험 감지 사유
     */
    @Column(name = "risk_reason", columnDefinition = "TEXT", nullable = false)
    private String riskReason;

    /**
     * 알림 확인 여부
     */
    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked = false;

    /**
     * 알림 확인 일시
     */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /**
     * 알림 확인한 교수 (해당 과목 담당 교수)
     */
    @ManyToOne
    @JoinColumn(name = "checked_by_professor_id")
    private Professor checkedByProfessor;

    @Column(name = "checked_by_professor_id", insertable = false, updatable = false)
    private Integer checkedByProfessorId;

    /**
     * 알림 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}