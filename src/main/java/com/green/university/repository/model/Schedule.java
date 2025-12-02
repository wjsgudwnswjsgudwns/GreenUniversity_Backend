package com.green.university.repository.model;

import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.green.university.repository.model.Staff;

import lombok.Data;

/**
 * 학사일정 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "schedule_tb")
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 일정을 등록한 교직원.
     */
    @ManyToOne
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "start_day", nullable = false)
    private Date startDay;

    @Column(name = "end_day", nullable = false)
    private Date endDay;

    @Column(name = "information", nullable = false)
    private String information;

    // years, months 필드는 테이블에 존재하지 않으므로 transient 처리하거나 제거할 수 있다.
}
