package com.green.university.repository;

import com.green.university.repository.model.AIAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AIAnalysisResultRepository extends JpaRepository<AIAnalysisResult, Integer> {

    /**
     * 학생 ID와 과목 ID로 최신 분석 결과 조회
     */
    @Query("SELECT a FROM AIAnalysisResult a WHERE a.studentId = :studentId AND a.subjectId = :subjectId ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findByStudentIdAndSubjectIdOrderByAnalyzedAtDesc(@Param("studentId") Integer studentId,
                                                                            @Param("subjectId") Integer subjectId);

    /**
     * 학생의 모든 과목 분석 결과 조회 (최신순)
     */
    List<AIAnalysisResult> findByStudentIdOrderByAnalyzedAtDesc(Integer studentId);

    /**
     * 특정 학생의 특정 연도/학기 분석 결과 조회
     */
    List<AIAnalysisResult> findByStudentIdAndAnalysisYearAndSemester(Integer studentId, Integer analysisYear, Integer semester);

    /**
     * 과목별 위험 학생 조회 (종합 위험도가 CAUTION, RISK, CRITICAL인 학생)
     */
    @Query("SELECT a FROM AIAnalysisResult a WHERE a.subjectId = :subjectId AND a.overallRisk IN ('CAUTION', 'RISK', 'CRITICAL') ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findRiskStudentsBySubjectId(@Param("subjectId") Integer subjectId);

    /**
     * 학과별 위험 학생 조회
     */
    @Query("SELECT a FROM AIAnalysisResult a JOIN a.student s WHERE s.deptId = :deptId AND a.overallRisk IN ('CAUTION', 'RISK', 'CRITICAL') ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findRiskStudentsByDeptId(@Param("deptId") Integer deptId);

    /**
     * 단과대별 위험 학생 조회
     */
    @Query("SELECT a FROM AIAnalysisResult a JOIN a.student s JOIN s.department d WHERE d.college.id = :collegeId AND a.overallRisk IN ('CAUTION', 'RISK', 'CRITICAL') ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findRiskStudentsByCollegeId(@Param("collegeId") Integer collegeId);

    /**
     * 전체 위험 학생 조회 (스태프용) - CAUTION 포함
     */
    @Query("SELECT a FROM AIAnalysisResult a WHERE a.overallRisk IN ('CAUTION', 'RISK', 'CRITICAL') ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findAllRiskStudents();

    /**
     * 전체 학생 분석 결과 조회 (모든 위험도 포함) - FETCH JOIN 추가
     */
    @Query("SELECT DISTINCT a FROM AIAnalysisResult a " +
            "LEFT JOIN FETCH a.student s " +
            "LEFT JOIN FETCH s.department d " +
            "LEFT JOIN FETCH d.college " +
            "LEFT JOIN FETCH a.subject sub " +
            "LEFT JOIN FETCH sub.professor " +
            "ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findAllWithRelations();

    /**
     * 전체 학생 분석 결과 조회 (기본)
     */
    @Query("SELECT a FROM AIAnalysisResult a ORDER BY a.analyzedAt DESC")
    List<AIAnalysisResult> findAllOrderByAnalyzedAtDesc();
}