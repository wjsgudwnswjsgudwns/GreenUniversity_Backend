package com.green.university.repository.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;

/**
 * 등록금 엔티티의 복합키를 표현하는 클래스입니다.
 */
@Data
@Embeddable
public class TuitionId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "tui_year")
    private Integer tuiYear;

    @Column(name = "semester")
    private Integer semester;
}