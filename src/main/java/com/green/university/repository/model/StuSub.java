package com.green.university.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Subject;

import lombok.Data;

/**
 * 수강 신청 내역을 나타내는 엔티티입니다.
 */
@Data
@Entity
@Table(name = "stu_sub_tb")
public class StuSub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 수강 내역이 속한 학생.
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    /**
     * 수강 내역에 해당하는 과목.
     */
    @ManyToOne
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "subject_id", insertable = false, updatable = false)
    private Integer subjectId;

    @Column(name = "grade")
    private String grade;

    @Column(name = "complete_grade")
    private Integer completeGrade;
}
