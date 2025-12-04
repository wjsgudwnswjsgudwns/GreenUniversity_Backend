package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 장학금 유형 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "scholarship_tb")
public class Scholarship {

    @Id
    @Column(name = "type")
    private Integer type;

    @Column(name = "max_amount", nullable = false)
    private Integer maxAmount;
}
