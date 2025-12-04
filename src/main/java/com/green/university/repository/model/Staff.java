package com.green.university.repository.model;

import java.sql.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 교직원 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "staff_tb")
public class Staff {

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

    @Column(name = "hire_date")
    private Date hireDate;
    
}
