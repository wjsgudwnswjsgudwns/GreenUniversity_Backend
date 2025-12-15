package com.green.university.repository.model;

import jakarta.persistence.*;


import lombok.Data;

// 추가 import: Department 엔티티를 참조하기 위해 필요
import com.green.university.repository.model.Department;

import java.util.List;

@Data
@Entity
@Table(name = "student_tb")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private java.sql.Date birthDate;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "tel", nullable = false)
    private String tel;

    @Column(name = "email", nullable = false)
    private String email;

    /**
     * 학생이 소속된 학과. {@link Department}와 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    /**
     * 학과 ID를 그대로 읽을 수 있도록 기존 필드를 남겨둡니다. JPA 연관관계를 통해 쓰기 작업이
     * 이루어지므로 insertable=false, updatable=false 속성으로 지정합니다. 이를 통해
     * 컨트롤러/서비스에서 getDeptId() 호출을 그대로 유지할 수 있습니다.
     */
    @Column(name = "dept_id", insertable = false, updatable = false)
    private Integer deptId;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "entrance_date", nullable = false)
    private java.sql.Date entranceDate;

    @Column(name = "graduation_date")
    private java.sql.Date graduationDate;

    @ManyToOne
    @JoinColumn(name = "advisor_id")
    private Professor advisor;

    @Column(name = "advisor_id", insertable = false, updatable = false)
    private Integer advisorId;

}
