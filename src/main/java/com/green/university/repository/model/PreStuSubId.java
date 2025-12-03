package com.green.university.repository.model;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@link PreStuSub} 엔티티의 복합 기본키를 정의하는 클래스입니다.
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PreStuSubId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "student_id")
    private Integer studentId;

    @Column(name = "subject_id")
    private Integer subjectId;
}
