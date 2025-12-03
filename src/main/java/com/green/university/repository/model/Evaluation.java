package com.green.university.repository.model;

import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Table(name="evaluation_tb")
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "evaluation_id")
    private Integer evaluationId;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "subject_id")
    private Integer subjectId;

    @Column(nullable = false)
    private Integer answer1;

    @Column(nullable = false)
    private Integer answer2;

    @Column(nullable = false)
    private Integer answer3;

    @Column(nullable = false)
    private Integer answer4;

    @Column(nullable = false)
    private Integer answer5;

    @Column(nullable = false)
    private Integer answer6;

    @Column(nullable = false)
    private Integer answer7;

    private String improvements;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

}
