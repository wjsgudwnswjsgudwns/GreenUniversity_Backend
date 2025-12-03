package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.green.university.repository.model.Scholarship;
import com.green.university.repository.model.StuSchId;

import lombok.Data;

/**
 * 학생의 학기별 장학금 유형을 나타내는 JPA 엔티티입니다.
 *
 * 복합키는 {@link StuSchId}를 사용하여 매핑하며, 장학금 유형은 {@link Scholarship}과 연관됩니다.
 */
@Data
@Entity
@Table(name = "stu_sch_tb")
public class StuSch {

    @EmbeddedId
    private StuSchId id;

    /**
     * 장학금 유형에 대한 연관관계.
     */
    @ManyToOne
    @JoinColumn(name = "sch_type")
    private Scholarship scholarship;

    /**
     * 장학금 유형을 참조하는 경우 필드도 보존한다. (선택)
     */
    @Column(name = "sch_type", insertable = false, updatable = false)
    private Integer schType;
}
