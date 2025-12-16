package com.green.university.controller;

import com.green.university.dto.AIResponseDTO;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.service.AIAnalysisResultService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-analysis")
@RequiredArgsConstructor
public class AIAnalysisResultController {

    private final AIAnalysisResultService aiAnalysisResultService;

    /**
     * 학생의 분석 결과 조회
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentAnalysisResults(@PathVariable Integer studentId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getStudentAnalysisResults(studentId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "학생 분석 결과 조회 성공", results));
    }

    /**
     * 학생-과목별 최신 분석 결과 조회
     */
    @GetMapping("/student/{studentId}/subject/{subjectId}")
    public ResponseEntity<?> getLatestAnalysisResult(
            @PathVariable Integer studentId,
            @PathVariable Integer subjectId) {
        AIAnalysisResult result = aiAnalysisResultService.getLatestAnalysisResult(studentId, subjectId);
        if (result == null) {
            return ResponseEntity.ok(new AIResponseDTO<>(0, "분석 결과가 없습니다.", null));
        }
        return ResponseEntity.ok(new AIResponseDTO<>(1, "최신 분석 결과 조회 성공", result));
    }

    /**
     * 과목별 위험 학생 조회 (교수용)
     */
    @GetMapping("/subject/{subjectId}/risk-students")
    public ResponseEntity<?> getRiskStudentsBySubject(@PathVariable Integer subjectId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getRiskStudentsBySubject(subjectId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "과목별 위험 학생 조회 성공", results));
    }

    /**
     * 학과별 위험 학생 조회 (스태프용)
     */
    @GetMapping("/department/{deptId}/risk-students")
    public ResponseEntity<?> getRiskStudentsByDept(@PathVariable Integer deptId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getRiskStudentsByDept(deptId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "학과별 위험 학생 조회 성공", results));
    }

    /**
     * 단과대별 위험 학생 조회 (스태프용)
     */
    @GetMapping("/college/{collegeId}/risk-students")
    public ResponseEntity<?> getRiskStudentsByCollege(@PathVariable Integer collegeId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getRiskStudentsByCollege(collegeId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "단과대별 위험 학생 조회 성공", results));
    }

    /**
     * 전체 위험 학생 조회 (스태프용) - CAUTION, RISK, CRITICAL
     */
    @GetMapping("/risk-students/all")
    public ResponseEntity<?> getAllRiskStudents() {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAllRiskStudents();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 위험 학생 조회 성공", results));
    }

    /**
     * 전체 학생 분석 결과 조회 (스태프용) - 모든 위험도 포함
     */
    @GetMapping("/students/all")
    public ResponseEntity<?> getAllStudents() {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAllStudents();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 학생 조회 성공", results));
    }

    /**
     * AI 분석 실행
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeStudent(@RequestBody AnalyzeRequest request) {
        AIAnalysisResult result = aiAnalysisResultService.analyzeStudent(
                request.getStudentId(),
                request.getSubjectId(),
                request.getYear(),
                request.getSemester()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AIResponseDTO<>(1, "AI 분석 실행 성공", result));
    }

    /**
     * 전체 학생-과목 일괄 AI 분석 실행 (관리자용)
     */
    @PostMapping("/analyze-all")
    public ResponseEntity<?> analyzeAllStudentsAndSubjects(@RequestBody AnalyzeBatchRequest request) {
        int count = aiAnalysisResultService.analyzeAllStudentsAndSubjects(
                request.getYear(),
                request.getSemester()
        );
        return ResponseEntity.ok(new AIResponseDTO<>(1, count + "건의 분석 완료", count));
    }

    /**
     * 교수 담당 학생 분석 결과 조회 (교수용) - 모든 위험도 포함
     */
    @GetMapping("/advisor/{advisorId}/students")
    public ResponseEntity<?> getAdvisorStudents(@PathVariable Integer advisorId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAdvisorStudents(advisorId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "담당 학생 조회 성공", results));
    }


    // Request DTO
    @lombok.Data
    static class AnalyzeBatchRequest {
        private Integer year;
        private Integer semester;
    }

    // Request DTOs
    @lombok.Data
    static class AnalyzeRequest {
        private Integer studentId;
        private Integer subjectId;
        private Integer year;
        private Integer semester;
    }

    @lombok.Data
    static class AnalyzeAllRequest {
        private Integer year;
        private Integer semester;
    }


}