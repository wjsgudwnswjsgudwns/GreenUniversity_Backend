package com.green.university.controller;

import com.green.university.dto.AIResponseDTO;
import com.green.university.repository.StudentJpaRepository;
import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Subject;
import com.green.university.service.RiskEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class EmailTestController {

    private final RiskEmailService riskEmailService;
    private final StudentJpaRepository studentRepository;
    private final SubjectJpaRepository subjectRepository;

    /**
     * 이메일 발송 테스트
     * POST /api/test/send-risk-email
     *
     * Request Body:
     * {
     *   "studentId": 2023000011,
     *   "subjectId": 1,
     *   "riskLevel": "CRITICAL" 또는 "RISK"
     * }
     */
    @PostMapping("/send-risk-email")
    public ResponseEntity<?> testSendRiskEmail(@RequestBody EmailTestRequest request) {
        try {
            Integer studentId = request.getStudentId();
            Integer subjectId = request.getSubjectId();
            String riskLevel = request.getRiskLevel();

            // 유효성 검사
            if (studentId == null || subjectId == null || riskLevel == null) {
                return ResponseEntity.badRequest()
                        .body(new AIResponseDTO<>(0, "학생 ID, 과목 ID, 위험도를 모두 입력해주세요.", null));
            }

            if (!riskLevel.equals("RISK") && !riskLevel.equals("CRITICAL")) {
                return ResponseEntity.badRequest()
                        .body(new AIResponseDTO<>(0, "위험도는 RISK 또는 CRITICAL이어야 합니다.", null));
            }

            // 학생 조회
            Student student = studentRepository.findById(studentId).orElse(null);
            if (student == null) {
                return ResponseEntity.badRequest()
                        .body(new AIResponseDTO<>(0, "학생을 찾을 수 없습니다. ID: " + studentId, null));
            }

            // 과목 조회
            Subject subject = subjectRepository.findById(subjectId).orElse(null);
            if (subject == null) {
                return ResponseEntity.badRequest()
                        .body(new AIResponseDTO<>(0, "과목을 찾을 수 없습니다. ID: " + subjectId, null));
            }

            // 테스트용 AIAnalysisResult 생성
            AIAnalysisResult testResult = createTestAnalysisResult(student, subject, riskLevel);

            // 이메일 발송
            log.info("테스트 이메일 발송 시작: 학생={}, 과목={}, 위험도={}",
                    student.getName(), subject.getName(), riskLevel);

            riskEmailService.sendRiskEmailToStudent(student, subject, riskLevel, testResult);

            if (student.getAdvisor() != null) {
                riskEmailService.sendRiskEmailToProfessor(student, subject, riskLevel, testResult);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("studentName", student.getName());
            result.put("studentEmail", student.getEmail());
            result.put("subjectName", subject.getName());
            result.put("riskLevel", riskLevel);
            result.put("advisorName", student.getAdvisor() != null ? student.getAdvisor().getName() : null);
            result.put("advisorEmail", student.getAdvisor() != null ? student.getAdvisor().getEmail() : null);

            return ResponseEntity.ok(new AIResponseDTO<>(1, "테스트 이메일이 발송되었습니다.", result));

        } catch (Exception e) {
            log.error("테스트 이메일 발송 실패: " + e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(new AIResponseDTO<>(0, "이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), null));
        }
    }

    /**
     * 테스트용 AIAnalysisResult 생성
     */
    private AIAnalysisResult createTestAnalysisResult(Student student, Subject subject, String riskLevel) {
        AIAnalysisResult result = new AIAnalysisResult();
        result.setStudentId(student.getId());
        result.setSubjectId(subject.getId());
        result.setStudent(student);
        result.setSubject(subject);
        result.setOverallRisk(riskLevel);

        // 테스트 데이터 설정
        if ("CRITICAL".equals(riskLevel)) {
            result.setAttendanceStatus("CRITICAL");
            result.setHomeworkStatus("RISK");
            result.setMidtermStatus("CAUTION");
            result.setFinalStatus("NORMAL");
            result.setTuitionStatus("CAUTION");
            result.setCounselingStatus("RISK");
            result.setAnalysisDetail("테스트 메일입니다.\n\n현재 출결 상태가 심각한 수준입니다. 결석 횟수가 기준을 초과했으며, 과제 제출률도 낮은 상태입니다. 조속한 상담이 필요합니다.");
        } else {
            result.setAttendanceStatus("CAUTION");
            result.setHomeworkStatus("RISK");
            result.setMidtermStatus("NORMAL");
            result.setFinalStatus("NORMAL");
            result.setTuitionStatus("NORMAL");
            result.setCounselingStatus("CAUTION");
            result.setAnalysisDetail("테스트 메일입니다.\n\n과제 제출에 어려움이 있는 것으로 보입니다. 지도교수님과의 상담을 통해 학업 방향을 점검해보시기 바랍니다.");
        }

        return result;
    }

    @lombok.Data
    static class EmailTestRequest {
        private Integer studentId;
        private Integer subjectId;
        private String riskLevel;
    }
}