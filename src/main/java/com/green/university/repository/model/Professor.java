package com.green.university.repository.model;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 교수 정보를 나타내는 JPA 엔티티.
 *
 * 교수는 하나의 학과에 소속되므로 {@link Department}와 N:1 관계를 갖는다.
 */
@Data
@Entity
@Table(name = "professor_tb")
public class Professor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private Date birthDate;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "tel", nullable = false)
    private String tel;

    @Column(name = "email", nullable = false)
    private String email;

    /**
     * 교수가 소속된 학과.
     */
    @ManyToOne
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    /**
     * 학과 ID를 그대로 읽을 수 있도록 기존 필드를 남겨둡니다. JPA 연관관계를 통해 쓰기
     * 작업이 이루어지므로 insertable=false, updatable=false 속성으로 지정합니다. 이를 통해
     * 컨트롤러/서비스에서 getDeptId() 호출을 그대로 유지할 수 있습니다.
     */
    @Column(name = "dept_id", insertable = false, updatable = false)
    private Integer deptId;

    @Column(name = "hire_date")
    private Date hireDate;
}
