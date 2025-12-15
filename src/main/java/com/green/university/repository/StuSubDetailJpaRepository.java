package com.green.university.repository;

import com.green.university.repository.model.StuSub;
import com.green.university.repository.model.StuSubDetail;
import com.green.university.repository.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StuSubDetailJpaRepository extends JpaRepository<StuSubDetail,Integer>{

    Optional<StuSubDetail> findByStudentIdAndSubjectId(Integer studentId, Integer subjectId);

    List<StuSubDetail> findByStudentId(Integer studentId);

    @Query("SELECT ssd FROM StuSubDetail ssd " +
            "JOIN ssd.subject s " +
            "WHERE ssd.studentId = :studentId " +
            "AND s.subYear = :year " +
            "AND s.semester = :semester")
    List<StuSubDetail> findByStudentIdAndYearAndSemester(
            @Param("studentId") Integer studentId,
            @Param("year") Integer year,
            @Param("semester") Integer semester);

    @Query("SELECT ssd FROM StuSubDetail ssd " +
            "JOIN FETCH ssd.stuSub ss " +
            "JOIN FETCH ssd.student s " +
            "JOIN FETCH s.department d " +
            "JOIN FETCH d.college " +
            "JOIN FETCH ssd.subject sub " +
            "JOIN FETCH sub.professor " +
            "WHERE ss.enrollmentType = 'ENROLLED'")
    List<StuSubDetail> findAllWithStudentAndSubject();

    /**
     * 학생 ID로 수강 과목 조회 (FETCH JOIN으로 N+1 문제 해결)
     */
    @Query("SELECT s FROM StuSubDetail s " +
            "LEFT JOIN FETCH s.stuSub ss " +
            "LEFT JOIN FETCH s.student st " +
            "LEFT JOIN FETCH st.department d " +
            "LEFT JOIN FETCH s.subject sub " +
            "LEFT JOIN FETCH sub.professor " +
            "WHERE s.studentId = :studentId " +
            "AND ss.enrollmentType = 'ENROLLED'")
    List<StuSubDetail> findByStudentIdWithRelations(@Param("studentId") Integer studentId);

}