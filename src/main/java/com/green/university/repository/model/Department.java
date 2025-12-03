package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.Data;

// 추가 import: College 엔티티를 참조하기 위해 필요
import com.green.university.repository.model.College;

@Data
@Entity
@Table(name = "department_tb")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 해당 학과가 소속된 단과대.
     * {@link College}와 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    private College college;
}
