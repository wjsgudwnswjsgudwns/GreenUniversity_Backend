package com.green.university.repository.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * 추천 과목 저장 엔티티
 */
@Entity
@Table(name = "recommended_subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendedSubjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "analysis_id", nullable = false)
    private Integer analysisId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    @Column(name = "recommendation_type", length = 20)
    private String recommendationType; // MAJOR, ELECTIVE

    @Column(name = "recommend_score")
    private Double recommendScore;

    @Column(name = "recommend_reason", columnDefinition = "TEXT")
    private String recommendReason;

    @Column(name = "display_order")
    private Integer displayOrder;

    // 분석과의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", insertable = false, updatable = false)
    private PersonalizedLearningAnalysis analysis;

    // 과목과의 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
}