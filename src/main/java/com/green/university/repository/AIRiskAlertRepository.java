package com.green.university.repository;

import com.green.university.repository.model.AIRiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AIRiskAlertRepository extends JpaRepository<AIRiskAlert, Integer> {

    /**
     * 미확인 알림 조회 (교수용 - 담당 과목별)
     */
    @Query("SELECT a FROM AIRiskAlert a WHERE a.subjectId = :subjectId AND a.isChecked = false ORDER BY a.createdAt DESC")
    List<AIRiskAlert> findUncheckedAlertsBySubjectId(@Param("subjectId") Integer subjectId);

    /**
     * 교수가 담당하는 과목들의 미확인 알림 조회
     */
    @Query("SELECT a FROM AIRiskAlert a JOIN a.subject s WHERE s.professor.id = :professorId AND a.isChecked = false ORDER BY a.createdAt DESC")
    List<AIRiskAlert> findUncheckedAlertsByProfessorId(@Param("professorId") Integer professorId);

    /**
     * 학생별 알림 조회
     */
    List<AIRiskAlert> findByStudentIdOrderByCreatedAtDesc(Integer studentId);

    /**
     * 학과별 미확인 알림 조회 (스태프용)
     */
    @Query("SELECT a FROM AIRiskAlert a JOIN a.student s WHERE s.deptId = :deptId AND a.isChecked = false ORDER BY a.createdAt DESC")
    List<AIRiskAlert> findUncheckedAlertsByDeptId(@Param("deptId") Integer deptId);

    /**
     * 단과대별 미확인 알림 조회 (스태프용)
     */
    @Query("SELECT a FROM AIRiskAlert a JOIN a.student s JOIN s.department d WHERE d.college.id = :collegeId AND a.isChecked = false ORDER BY a.createdAt DESC")
    List<AIRiskAlert> findUncheckedAlertsByCollegeId(@Param("collegeId") Integer collegeId);

    /**
     * 전체 미확인 알림 조회 (스태프용)
     */
    List<AIRiskAlert> findByIsCheckedFalseOrderByCreatedAtDesc();

    /**
     * 특정 위험도 이상의 알림 조회
     */
    @Query("SELECT a FROM AIRiskAlert a WHERE a.riskLevel IN :riskLevels AND a.isChecked = false ORDER BY a.createdAt DESC")
    List<AIRiskAlert> findByRiskLevelsAndUnchecked(@Param("riskLevels") List<String> riskLevels);
}