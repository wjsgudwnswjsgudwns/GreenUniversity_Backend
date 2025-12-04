package com.green.university.repository.model;

import java.sql.Date;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.BreakApp;

import lombok.Data;

@Data
@Entity
@Table(name = "stu_stat_tb")
public class StuStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 학적 상태가 속하는 학생. {@link Student}과 연관관계.
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "from_date")
    private Date fromDate;

    @Column(name = "to_date")
    private Date toDate;

    /**
     * 휴학 신청과 연관된 경우 {@link BreakApp}을 참조한다.
     */
    @ManyToOne
    @JoinColumn(name = "break_app_id")
    private BreakApp breakApp;
}
