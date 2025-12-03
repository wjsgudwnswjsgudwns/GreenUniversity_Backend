package com.green.university.repository.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Scholarship;
import com.green.university.repository.model.TuitionId;

import com.green.university.utils.NumberUtil;

import lombok.Data;

/**
 * 등록금 정보를 나타내는 JPA 엔티티입니다.
 *
 * 복합키는 {@link TuitionId}로 구성되며, 학생 및 장학금과 연관된다.
 */
@Data
@Entity
@Table(name = "tuition_tb")
public class Tuition {

    @EmbeddedId
    private TuitionId id;

    @ManyToOne
    @JoinColumn(name = "student_id", insertable = false, updatable = false)
    private Student student;

    @ManyToOne
    @JoinColumn(name = "sch_type", insertable = false, updatable = false)
    private Scholarship scholarship;

    @Column(name = "tui_amount", nullable = false)
    private Integer tuiAmount;

    @Column(name = "sch_type")
    private Integer schType;

    @Column(name = "sch_amount")
    private Integer schAmount;

    @Column(name = "status")
    private Boolean status;

    public String tuiFormat() {
        return NumberUtil.numberFormat(tuiAmount);
    }

    public String schFormat() {
        return NumberUtil.numberFormat(schAmount);
    }

    public String paymentFormat() {
        Integer payAmount = tuiAmount - (schAmount != null ? schAmount : 0);
        return NumberUtil.numberFormat(payAmount);
    }

    public Tuition() {
        // 기본 생성자
    }

    public Tuition(TuitionId id, Integer tuiAmount, Integer schType, Integer schAmount) {
        this.id = id;
        this.tuiAmount = tuiAmount;
        this.schType = schType;
        this.schAmount = schAmount;
    }
}
