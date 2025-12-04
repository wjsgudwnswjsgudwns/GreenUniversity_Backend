package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 학점과 환산 점수 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "grade_tb")
public class Grade {

    @Id
    @Column(name = "grade")
    private String grade;

    @Column(name = "grade_value", nullable = false)
    private Float gradeValue;
}
