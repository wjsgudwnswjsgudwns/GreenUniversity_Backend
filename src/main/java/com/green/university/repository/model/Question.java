package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 강의평가 질문 정보를 나타내는 JPA 엔티티.
 *
 * question_tb 테이블과 매핑되며, 각 질문 내용 및 제안 사항을 보관한다.
 */
@Data
@Entity
@Table(name = "question_tb")
public class Question {
    /**
     * 질문 ID. AUTO_INCREMENT 컬럼으로 설정되어 있다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "question1", nullable = false, length = 100)
    private String question1;

    @Column(name = "question2", nullable = false, length = 100)
    private String question2;

    @Column(name = "question3", nullable = false, length = 100)
    private String question3;

    @Column(name = "question4", nullable = false, length = 100)
    private String question4;

    @Column(name = "question5", nullable = false, length = 100)
    private String question5;

    @Column(name = "question6", nullable = false, length = 100)
    private String question6;

    @Column(name = "question7", nullable = false, length = 100)
    private String question7;

    /**
     * 제안 내용. 원본 컬럼은 sug_content이며 카멜케이스로 매핑한다.
     */
    @Column(name = "sug_content", nullable = false, length = 255)
    private String sugContent;
}