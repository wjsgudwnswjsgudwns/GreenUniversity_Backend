package com.green.university.repository.model;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.Data;

/**
 * 예비 수강신청 테이블(pre_stu_sub_tb)의 JPA 매핑 클래스입니다.
 * 복합 기본키를 표현하기 위해 {@link PreStuSubId}를 사용합니다.
 */
@Data
@Entity
@Table(name = "pre_stu_sub_tb")
public class PreStuSub implements Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private PreStuSubId id;

    public PreStuSub() {
    }

    public PreStuSub(Integer studentId, Integer subjectId) {
        this.id = new PreStuSubId(studentId, subjectId);
    }

    public Integer getStudentId() {
        return id != null ? id.getStudentId() : null;
    }

    public Integer getSubjectId() {
        return id != null ? id.getSubjectId() : null;
    }
}
