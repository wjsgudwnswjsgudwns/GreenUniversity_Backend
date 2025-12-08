package com.green.university.repository.model;

import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.*;

@Entity
@Table(name = "stu_sub_detail_tb")
@Getter
@Setter
public class StuSubDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "subject_id", nullable = false)
    private Integer subjectId;

    private Integer absent;

    private Integer lateness;

    private Integer homework;

    @Column(name = "mid_exam")
    private Integer midExam;

    @Column(name = "final_exam")
    private Integer finalExam;

    @Column(name = "converted_mark")
    private Integer convertedMark;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id", insertable = false, updatable = false)
    private StuSub stuSub;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;
}
