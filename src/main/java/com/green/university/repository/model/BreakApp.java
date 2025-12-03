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

import com.green.university.utils.DateUtil;

import lombok.Data;

/**
 * 휴학 신청 내역을 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "break_app_tb")
public class BreakApp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 휴학 신청을 한 학생.
     */
    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "student_id", insertable = false, updatable = false)
    private Integer studentId;

    @Column(name = "student_grade")
    private Integer studentGrade;

    @Column(name = "from_year")
    private Integer fromYear;

    @Column(name = "from_semester")
    private Integer fromSemester;

    @Column(name = "to_year")
    private Integer toYear;

    @Column(name = "to_semester")
    private Integer toSemester;

    @Column(name = "type")
    private String type;

    @Column(name = "app_date")
    private Date appDate;

    @Column(name = "status")
    private String status;

    public String appDateFormat() {
        return DateUtil.dateFormat(appDate);
    }
}
