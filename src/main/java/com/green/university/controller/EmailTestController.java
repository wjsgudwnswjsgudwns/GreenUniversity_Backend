package com.green.university.controller;

import com.green.university.repository.AIAnalysisResultRepository;
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
@RequestMapping("/api/email-test")
@RequiredArgsConstructor
public class EmailTestController {

    private final RiskEmailService riskEmailService;
    private final AIAnalysisResultRepository aiAnalysisResultRepository;
    private final StudentJpaRepository studentRepository;
    private final SubjectJpaRepository subjectRepository;

    /**
     * 특정 학생에게 위험 알림 이메일 전송 테스트
     * POST /api/email-test/send-to-student
     */
    @PostMapping("/send-to-student")
    public ResponseEntity<?> sendEmailToStudent(@RequestBody Map<String, Integer> request) {
        try {
            Integer analysisResultId = request.get("analysisResultId");

            if (analysisResultId == null) {
                return ResponseEntity.badRequest().body(
                        createResponse(false, "분석 결과 ID가 필요합니다.", null)
                );
            }

            // AI 분석 결과 조회
            AIAnalysisResult result = aiAnalysisResultRepository.findById(analysisResultId)
                    .orElseThrow(() -> new RuntimeException("분석 결과를 찾을 수 없습니다."));

            // 학생 정보 조회
            Student student = studentRepository.findById(result.getStudentId())
                    .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

            // 과목 정보 조회
            Subject subject = subjectRepository.findById(result.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("과목 정보를 찾을 수 없습니다."));

            // 이메일 발송
            riskEmailService.sendRiskEmailToStudent(
                    student,
                    subject,
                    result.getOverallRisk(),
                    result
            );

            log.info("학생 이메일 전송 완료: 학생={}, 이메일={}", student.getName(), student.getEmail());

            return ResponseEntity.ok(
                    createResponse(true, "학생에게 이메일이 발송되었습니다.",
                            Map.of("studentEmail", student.getEmail()))
            );

        } catch (Exception e) {
            log.error("학생 이메일 발송 실패", e);
            return ResponseEntity.status(500).body(
                    createResponse(false, "이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 특정 학생의 지도교수에게 위험 알림 이메일 전송 테스트
     * POST /api/email-test/send-to-professor
     */
    @PostMapping("/send-to-professor")
    public ResponseEntity<?> sendEmailToProfessor(@RequestBody Map<String, Integer> request) {
        try {
            Integer analysisResultId = request.get("analysisResultId");

            if (analysisResultId == null) {
                return ResponseEntity.badRequest().body(
                        createResponse(false, "분석 결과 ID가 필요합니다.", null)
                );
            }

            // AI 분석 결과 조회
            AIAnalysisResult result = aiAnalysisResultRepository.findById(analysisResultId)
                    .orElseThrow(() -> new RuntimeException("분석 결과를 찾을 수 없습니다."));

            // 학생 정보 조회
            Student student = studentRepository.findById(result.getStudentId())
                    .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

            // 과목 정보 조회
            Subject subject = subjectRepository.findById(result.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("과목 정보를 찾을 수 없습니다."));

            // 지도교수 확인
            if (student.getAdvisor() == null) {
                return ResponseEntity.badRequest().body(
                        createResponse(false, "해당 학생의 지도교수가 지정되지 않았습니다.", null)
                );
            }

            // 이메일 발송
            riskEmailService.sendRiskEmailToProfessor(
                    student,
                    subject,
                    result.getOverallRisk(),
                    result
            );

            log.info("교수 이메일 전송 완료: 교수={}, 이메일={}",
                    student.getAdvisor().getName(), student.getAdvisor().getEmail());

            return ResponseEntity.ok(
                    createResponse(true, "지도교수에게 이메일이 발송되었습니다.",
                            Map.of("professorEmail", student.getAdvisor().getEmail()))
            );

        } catch (Exception e) {
            log.error("교수 이메일 발송 실패", e);
            return ResponseEntity.status(500).body(
                    createResponse(false, "이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 학생과 지도교수 모두에게 이메일 발송
     * POST /api/email-test/send-both
     */
    @PostMapping("/send-both")
    public ResponseEntity<?> sendEmailToBoth(@RequestBody Map<String, Integer> request) {
        try {
            Integer analysisResultId = request.get("analysisResultId");

            if (analysisResultId == null) {
                return ResponseEntity.badRequest().body(
                        createResponse(false, "분석 결과 ID가 필요합니다.", null)
                );
            }

            // AI 분석 결과 조회
            AIAnalysisResult result = aiAnalysisResultRepository.findById(analysisResultId)
                    .orElseThrow(() -> new RuntimeException("분석 결과를 찾을 수 없습니다."));

            // 학생 정보 조회
            Student student = studentRepository.findById(result.getStudentId())
                    .orElseThrow(() -> new RuntimeException("학생 정보를 찾을 수 없습니다."));

            // 과목 정보 조회
            Subject subject = subjectRepository.findById(result.getSubjectId())
                    .orElseThrow(() -> new RuntimeException("과목 정보를 찾을 수 없습니다."));

            Map<String, String> sentEmails = new HashMap<>();

            // 학생에게 이메일 발송
            riskEmailService.sendRiskEmailToStudent(student, subject, result.getOverallRisk(), result);
            sentEmails.put("studentEmail", student.getEmail());
            log.info("학생 이메일 전송 완료: {}", student.getEmail());

            // 지도교수에게 이메일 발송 (있는 경우)
            if (student.getAdvisor() != null) {
                riskEmailService.sendRiskEmailToProfessor(student, subject, result.getOverallRisk(), result);
                sentEmails.put("professorEmail", student.getAdvisor().getEmail());
                log.info("교수 이메일 전송 완료: {}", student.getAdvisor().getEmail());
            } else {
                sentEmails.put("professorEmail", "지도교수 미지정");
            }

            return ResponseEntity.ok(
                    createResponse(true, "이메일이 성공적으로 발송되었습니다.", sentEmails)
            );

        } catch (Exception e) {
            log.error("이메일 발송 실패", e);
            return ResponseEntity.status(500).body(
                    createResponse(false, "이메일 발송 중 오류가 발생했습니다: " + e.getMessage(), null)
            );
        }
    }

    /**
     * 응답 객체 생성 헬퍼 메서드
     */
    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        return response;
    }
}