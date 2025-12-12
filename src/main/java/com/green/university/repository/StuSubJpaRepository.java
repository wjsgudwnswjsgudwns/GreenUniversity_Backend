package com.green.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.green.university.repository.model.StuSub;

/**
 * JPA repository for {@link StuSub} entities.
 */
public interface StuSubJpaRepository extends JpaRepository<StuSub, Integer> {

    /**
     * 학생의 수강신청 내역 중 특정 과목을 조회한다.
     */
//    StuSub findByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

    /**
     * 학생의 이번 학기 수강신청 내역을 모두 조회한다.
     */
    @Query("SELECT s FROM StuSub s WHERE s.studentId = :studentId")
    List<StuSub> findAllByStudentId(@Param("studentId") Integer studentId);

    /**
     * 수강신청 내역 삭제
     */
    void deleteByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

    List<StuSub> findBySubjectId(Integer subjectId);

    Optional<StuSub> findByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

    List<StuSub> findByStudentIdAndSubject_SubYearAndSubject_Semester(Integer studentId, Integer subYear, Integer semester);

    List<StuSub> findByStudentId(Integer studentId);

    List<StuSub> findByStudentIdAndSubject_SubYearAndSubject_SemesterAndSubject_Type(Integer studentId, Integer subYear, Integer semester, String type);

}