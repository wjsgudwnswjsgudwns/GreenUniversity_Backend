package com.green.university.repository;

import com.green.university.repository.model.AICounseling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AICounselingRepository extends JpaRepository<AICounseling, Integer> {

    /**
     * 학생 ID로 상담 일정 조회 (학생용)
     */
    List<AICounseling> findByStudentIdOrderByScheduledAtDesc(Integer studentId);

    /**
     * 교수 ID로 상담 일정 조회
     */
    List<AICounseling> findByProfessorIdOrderByScheduledAtDesc(Integer professorId);

    /**
     * 과목 ID로 상담 일정 조회
     */
    List<AICounseling> findBySubjectIdOrderByScheduledAtDesc(Integer subjectId);

    /**
     * 교수가 담당하는 과목의 학생별 상담 내역 조회
     */
    @Query("SELECT c FROM AICounseling c WHERE c.professorId = :professorId AND c.studentId = :studentId ORDER BY c.scheduledAt DESC")
    List<AICounseling> findByProfessorIdAndStudentId(@Param("professorId") Integer professorId,
                                                     @Param("studentId") Integer studentId);

    /**
     * 완료되지 않은 상담 일정 조회
     */
    List<AICounseling> findByStudentIdAndIsCompletedFalseOrderByScheduledAtAsc(Integer studentId);

    /**
     * 완료된 상담 중 내용이 작성된 상담 조회 (AI 분석 대상)
     */
    @Query("SELECT c FROM AICounseling c WHERE c.isCompleted = true AND c.counselingContent IS NOT NULL AND c.studentId = :studentId")
    List<AICounseling> findCompletedCounselingsWithContentByStudentId(@Param("studentId") Integer studentId);

    /**
     * 완료된 상담 중 내용이 작성된 상담 조회 (AI 분석 대상) - 과목별
     */
    @Query("SELECT c FROM AICounseling c WHERE c.isCompleted = true AND c.counselingContent IS NOT NULL AND c.studentId = :studentId AND c.subjectId = :subjectId ORDER BY c.completedAt DESC")
    List<AICounseling> findCompletedCounselingsWithContentByStudentIdAndSubjectId(
            @Param("studentId") Integer studentId,
            @Param("subjectId") Integer subjectId);
}