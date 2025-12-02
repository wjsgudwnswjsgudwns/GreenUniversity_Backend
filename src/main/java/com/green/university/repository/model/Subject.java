package com.green.university.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import lombok.Data;

// 연관관계 엔티티를 참조하기 위한 import
import com.green.university.repository.model.Department;
import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Room;

@Data
@Entity
@Table(name = "subject_tb")
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    /**
     * 강의를 담당하는 교수. {@link Professor}와 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "professor_id", nullable = false)
    private Professor professor;

    /**
     * 강의가 열리는 강의실. {@link Room}과 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    /**
     * 강의가 속한 학과. {@link Department}와 N:1 관계를 맺는다.
     */
    @ManyToOne
    @JoinColumn(name = "dept_id", nullable = false)
    private Department department;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "sub_year", nullable = false)
    private Integer subYear;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "sub_day", nullable = false)
    private String subDay;

    @Column(name = "start_time", nullable = false)
    private Integer startTime;

    @Column(name = "end_time", nullable = false)
    private Integer endTime;

    @Column(name = "grades", nullable = false)
    private Integer grades;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "num_of_student")
    private Integer numOfStudent;
}
