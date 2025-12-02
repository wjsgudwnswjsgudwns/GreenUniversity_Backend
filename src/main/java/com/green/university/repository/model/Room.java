package com.green.university.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import com.green.university.repository.model.College;

import lombok.Data;

/**
 * 강의실 정보를 나타내는 JPA 엔티티. 강의실은 하나의 단과대에 소속된다.
 */
@Data
@Entity
@Table(name = "room_tb")
public class Room {

    @Id
    @Column(name = "id")
    private String id;

    /**
     * 강의실이 소속된 단과대.
     */
    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    private College college;
    
}
