package com.green.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.green.university.repository.model.PreStuSub;
import com.green.university.repository.model.PreStuSubId;

/**
 * JPA repository for {@link PreStuSub} entities.
 */
public interface PreStuSubJpaRepository extends JpaRepository<PreStuSub, PreStuSubId> {

    /**
     * 학생의 예비 수강신청 내역 중 특정 과목을 조회한다.
     */
    PreStuSub findByIdStudentIdAndIdSubjectId(Integer studentId, Integer subjectId);

    /**
     * 학생의 이번 학기 전체 예비 수강 신청 내역을 조회한다.
     */
    @Query("SELECT p FROM PreStuSub p WHERE p.id.studentId = :studentId")
    List<PreStuSub> findAllByStudentId(@Param("studentId") Integer studentId);

    /**
     * 학생의 예비 수강신청 내역 삭제
     */
    void deleteByIdStudentIdAndIdSubjectId(Integer studentId, Integer subjectId);

    List<PreStuSub> findByStudentIdAndSubject_SubYearAndSubject_Semester(Integer studentId, Integer subYear, Integer semester);

    List<PreStuSub> findByIdStudentId(Integer studentId);

    List<PreStuSub> findByIdSubjectId(Integer subjectId);
}