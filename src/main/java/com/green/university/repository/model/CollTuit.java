package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 단과대별 등록금 정보를 나타내는 엔티티입니다.
 */
@Data
@Entity
@Table(name = "coll_tuit_tb")
public class CollTuit {

    @Id
    @Column(name = "college_id")
    private Integer collegeId;

    @Column(name = "amount", nullable = false)
    private Integer amount;
}
