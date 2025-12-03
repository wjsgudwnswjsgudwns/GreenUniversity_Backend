package com.green.university.repository.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;

/**
 * 복합키: 학생별 장학금 엔티티의 식별자.
 */
@Data
@Embeddable
public class StuSchId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "sch_year")
    private Integer schYear;

    @Column(name = "semester")
    private Integer semester;
}