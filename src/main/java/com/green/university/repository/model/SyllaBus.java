package com.green.university.repository.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import com.green.university.repository.model.Subject;

import lombok.Data;

/**
 * 강의 계획서 정보를 나타내는 JPA 엔티티입니다.
 */
@Data
@Entity
@Table(name = "syllabus_tb")
public class SyllaBus {

    /**
     * 강의 계획서가 속한 과목. subject_id가 기본키로 사용된다.
     */
    @Id
    @Column(name = "subject_id")
    private Integer subjectId;

    @OneToOne
    @JoinColumn(name = "subject_id", insertable = false, updatable = false)
    private Subject subject;

    @Column(name = "overview")
    private String overview;

    @Column(name = "objective")
    private String objective;

    @Column(name = "textbook")
    private String textbook;

    @Column(name = "program", columnDefinition = "TEXT")
    private String program;
}
