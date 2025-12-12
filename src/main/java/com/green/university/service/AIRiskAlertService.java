package com.green.university.service;

import com.green.university.repository.AIRiskAlertRepository;
import com.green.university.repository.model.AIRiskAlert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AIRiskAlertService {

    private final AIRiskAlertRepository aiRiskAlertRepository;

    /**
     * 교수의 미확인 알림 조회
     */
    public List<AIRiskAlert> getUncheckedAlertsByProfessor(Integer professorId) {
        return aiRiskAlertRepository.findUncheckedAlertsByProfessorId(professorId);
    }

    /**
     * 과목별 미확인 알림 조회
     */
    public List<AIRiskAlert> getUncheckedAlertsBySubject(Integer subjectId) {
        return aiRiskAlertRepository.findUncheckedAlertsBySubjectId(subjectId);
    }

    /**
     * 학생별 알림 조회
     */
    public List<AIRiskAlert> getAlertsByStudent(Integer studentId) {
        return aiRiskAlertRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    /**
     * 학과별 미확인 알림 조회 (스태프용)
     */
    public List<AIRiskAlert> getUncheckedAlertsByDept(Integer deptId) {
        return aiRiskAlertRepository.findUncheckedAlertsByDeptId(deptId);
    }

    /**
     * 단과대별 미확인 알림 조회 (스태프용)
     */
    public List<AIRiskAlert> getUncheckedAlertsByCollege(Integer collegeId) {
        return aiRiskAlertRepository.findUncheckedAlertsByCollegeId(collegeId);
    }

    /**
     * 전체 미확인 알림 조회 (스태프용)
     */
    public List<AIRiskAlert> getAllUncheckedAlerts() {
        return aiRiskAlertRepository.findByIsCheckedFalseOrderByCreatedAtDesc();
    }

    /**
     * 고위험 알림만 조회 (RISK, CRITICAL)
     */
    public List<AIRiskAlert> getHighRiskAlerts() {
        return aiRiskAlertRepository.findByRiskLevelsAndUnchecked(Arrays.asList("RISK", "CRITICAL"));
    }

    /**
     * 알림 생성
     */
    @Transactional
    public AIRiskAlert createAlert(AIRiskAlert alert) {
        return aiRiskAlertRepository.save(alert);
    }

    /**
     * 알림 확인 처리
     */
    @Transactional
    public AIRiskAlert checkAlert(Integer alertId, Integer professorId) {
        AIRiskAlert alert = aiRiskAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        alert.setIsChecked(true);
        alert.setCheckedAt(LocalDateTime.now());
        alert.setCheckedByProfessorId(professorId);

        return aiRiskAlertRepository.save(alert);
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteAlert(Integer alertId) {
        aiRiskAlertRepository.deleteById(alertId);
    }

    /**
     * 일괄 알림 확인 처리
     */
    @Transactional
    public void checkAllAlerts(List<Integer> alertIds, Integer professorId) {
        for (Integer alertId : alertIds) {
            checkAlert(alertId, professorId);
        }
    }
}