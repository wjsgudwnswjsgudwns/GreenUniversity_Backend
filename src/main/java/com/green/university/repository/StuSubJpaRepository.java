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

    /**
     * 특정 과목의 수강신청 학생 조회 (FETCH JOIN으로 N+1 문제 해결)
     * stu_sub_tb 기준 - 실제 수강신청한 학생만
     */
    @Query("SELECT s FROM StuSub s " +
            "LEFT JOIN FETCH s.student st " +
            "LEFT JOIN FETCH st.department " +
            "WHERE s.subjectId = :subjectId")
    List<StuSub> findBySubjectIdWithStudent(@Param("subjectId") Integer subjectId);

    @Query("SELECT s FROM StuSub s " +
            "LEFT JOIN FETCH s.student st " +
            "LEFT JOIN FETCH st.department " +
            "WHERE s.subjectId = :subjectId " +
            "AND s.enrollmentType = 'ENROLLED'")
    List<StuSub> findEnrolledBySubjectId(@Param("subjectId") Integer subjectId);

    /**
     * ✅ 학생의 본 수강신청 내역만 조회
     */
    @Query("SELECT s FROM StuSub s " +
            "WHERE s.studentId = :studentId " +
            "AND s.enrollmentType = 'ENROLLED'")
    List<StuSub> findEnrolledByStudentId(@Param("studentId") Integer studentId);

    /**
     * ✅ 학생의 예비 수강신청 내역만 조회
     */
    @Query("SELECT s FROM StuSub s " +
            "WHERE s.studentId = :studentId " +
            "AND s.enrollmentType = 'PRE'")
    List<StuSub> findPreEnrollmentByStudentId(@Param("studentId") Integer studentId);

    List<StuSub> findBySubjectIdAndEnrollmentType(Integer subjectId, String enrollmentType);

}