package com.green.university.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
