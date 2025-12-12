package com.green.university.controller;

import com.green.university.dto.AIResponseDTO;
import com.green.university.repository.model.AIRiskAlert;
import com.green.university.service.AIRiskAlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-risk-alert")
@RequiredArgsConstructor
public class AIRiskAlertController {

    private final AIRiskAlertService aiRiskAlertService;

    /**
     * 교수의 미확인 알림 조회
     */
    @GetMapping("/professor/{professorId}/unchecked")
    public ResponseEntity<?> getUncheckedAlertsByProfessor(@PathVariable Integer professorId) {
        List<AIRiskAlert> alerts = aiRiskAlertService.getUncheckedAlertsByProfessor(professorId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "교수 미확인 알림 조회 성공", alerts));
    }

    /**
     * 과목별 미확인 알림 조회
     */
    @GetMapping("/subject/{subjectId}/unchecked")
    public ResponseEntity<?> getUncheckedAlertsBySubject(@PathVariable Integer subjectId) {
        List<AIRiskAlert> alerts = aiRiskAlertService.getUncheckedAlertsBySubject(subjectId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "과목별 미확인 알림 조회 성공", alerts));
    }

    /**
     * 학생별 알림 조회
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getAlertsByStudent(@PathVariable Integer studentId) {
        List<AIRiskAlert> alerts = aiRiskAlertService.getAlertsByStudent(studentId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "학생별 알림 조회 성공", alerts));
    }

    /**
     * 학과별 미확인 알림 조회 (스태프용)
     */
    @GetMapping("/department/{deptId}/unchecked")
    public ResponseEntity<?> getUncheckedAlertsByDept(@PathVariable Integer deptId) {
        List<AIRiskAlert> alerts = aiRiskAlertService.getUncheckedAlertsByDept(deptId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "학과별 미확인 알림 조회 성공", alerts));
    }

    /**
     * 단과대별 미확인 알림 조회 (스태프용)
     */
    @GetMapping("/college/{collegeId}/unchecked")
    public ResponseEntity<?> getUncheckedAlertsByCollege(@PathVariable Integer collegeId) {
        List<AIRiskAlert> alerts = aiRiskAlertService.getUncheckedAlertsByCollege(collegeId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "단과대별 미확인 알림 조회 성공", alerts));
    }

    /**
     * 전체 미확인 알림 조회 (스태프용)
     */
    @GetMapping("/unchecked/all")
    public ResponseEntity<?> getAllUncheckedAlerts() {
        List<AIRiskAlert> alerts = aiRiskAlertService.getAllUncheckedAlerts();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 미확인 알림 조회 성공", alerts));
    }

    /**
     * 고위험 알림만 조회
     */
    @GetMapping("/high-risk")
    public ResponseEntity<?> getHighRiskAlerts() {
        List<AIRiskAlert> alerts = aiRiskAlertService.getHighRiskAlerts();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "고위험 알림 조회 성공", alerts));
    }

    /**
     * 알림 생성
     */
    @PostMapping
    public ResponseEntity<?> createAlert(@RequestBody AIRiskAlert alert) {
        AIRiskAlert created = aiRiskAlertService.createAlert(alert);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AIResponseDTO<>(1, "알림 생성 성공", created));
    }

    /**
     * 알림 확인 처리
     */
    @PutMapping("/{alertId}/check")
    public ResponseEntity<?> checkAlert(
            @PathVariable Integer alertId,
            @RequestBody CheckAlertRequest request) {
        AIRiskAlert checked = aiRiskAlertService.checkAlert(alertId, request.getProfessorId());
        return ResponseEntity.ok(new AIResponseDTO<>(1, "알림 확인 처리 성공", checked));
    }

    /**
     * 알림 삭제
     */
    @DeleteMapping("/{alertId}")
    public ResponseEntity<?> deleteAlert(@PathVariable Integer alertId) {
        aiRiskAlertService.deleteAlert(alertId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "알림 삭제 성공", null));
    }

    /**
     * 일괄 알림 확인 처리
     */
    @PutMapping("/check-multiple")
    public ResponseEntity<?> checkAllAlerts(@RequestBody CheckMultipleAlertsRequest request) {
        aiRiskAlertService.checkAllAlerts(request.getAlertIds(), request.getProfessorId());
        return ResponseEntity.ok(new AIResponseDTO<>(1, "일괄 알림 확인 처리 성공", null));
    }

    // Request DTOs
    @lombok.Data
    static class CheckAlertRequest {
        private Integer professorId;
    }

    @lombok.Data
    static class CheckMultipleAlertsRequest {
        private List<Integer> alertIds;
        private Integer professorId;
    }
}