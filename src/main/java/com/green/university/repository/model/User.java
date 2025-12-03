package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 사용자 인증 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "user_tb")
public class User {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "user_role", nullable = false)
    private String userRole;

}
