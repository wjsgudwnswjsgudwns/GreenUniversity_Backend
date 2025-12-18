package com.green.university.repository.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 맞춤형 학습 분석 결과 저장 엔티티
 */
@Entity
@Table(name = "personalized_learning_analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalizedLearningAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    // === 학습 이력 분석 데이터 ===
    @Column(name = "overall_gpa")
    private Double overallGPA;

    @Column(name = "major_gpa")
    private Double majorGPA;

    @Column(name = "elective_gpa")
    private Double electiveGPA;

    @Column(name = "recent_gpa")
    private Double recentGPA;

    @Column(name = "total_credits")
    private Integer totalCredits;

    @Column(name = "major_credits")
    private Integer majorCredits;

    @Column(name = "elective_credits")
    private Integer electiveCredits;

    @Column(name = "grade_trend", length = 50)
    private String gradeTrend;

    @Column(name = "attendance_rate")
    private Double attendanceRate;

    @Column(name = "homework_rate")
    private Double homeworkRate;

    @Column(name = "strong_areas", columnDefinition = "TEXT")
    private String strongAreas; // JSON 배열로 저장

    @Column(name = "weak_areas", columnDefinition = "TEXT")
    private String weakAreas; // JSON 배열로 저장

    // === 학습 방향 데이터 ===
    @Column(name = "strengths", columnDefinition = "TEXT")
    private String strengths;

    @Column(name = "weaknesses", columnDefinition = "TEXT")
    private String weaknesses;

    @Column(name = "improvement_suggestions", columnDefinition = "TEXT")
    private String improvementSuggestions; // JSON 배열

    @Column(name = "learning_strategies", columnDefinition = "TEXT")
    private String learningStrategies; // JSON 배열

    @Column(name = "cautions", columnDefinition = "TEXT")
    private String cautions; // JSON 배열

    @Column(name = "recommended_pattern", length = 100)
    private String recommendedPattern;

    // === 졸업 요건 데이터 ===
    @Column(name = "total_required_credits")
    private Integer totalRequiredCredits;

    @Column(name = "remaining_credits")
    private Integer remainingCredits;

    @Column(name = "major_required_credits")
    private Integer majorRequiredCredits;

    @Column(name = "major_remaining_credits")
    private Integer majorRemainingCredits;

    @Column(name = "elective_required_credits")
    private Integer electiveRequiredCredits;

    @Column(name = "elective_remaining_credits")
    private Integer electiveRemainingCredits;

    @Column(name = "can_graduate")
    private Boolean canGraduate;

    @Column(name = "semesters_to_graduation")
    private Integer semestersToGraduation;

    @Column(name = "recommended_credits_per_semester")
    private Integer recommendedCreditsPerSemester;

    // === AI 분석 코멘트 ===
    @Column(name = "ai_analysis_comment", columnDefinition = "TEXT")
    private String aiAnalysisComment;

    // === 분석 시점 정보 ===
    @Column(name = "analysis_year")
    private Integer analysisYear;

    @Column(name = "analysis_semester")
    private Integer analysisSemester;

    @Column(name = "is_latest")
    private Boolean isLatest; // 최신 분석 여부

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 학생과의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;
}