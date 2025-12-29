package com.green.university.controller;

import com.green.university.dto.AIResponseDTO;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.service.AIAnalysisResultService;
import com.green.university.service.MultiAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai-analysis")
@RequiredArgsConstructor
public class AIAnalysisResultController {

    private final AIAnalysisResultService aiAnalysisResultService;
    private final MultiAIService multiAIService;

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
     * 전체 위험 학생 조회 (스태프용) - CAUTION, RISK, CRITICAL (기존 메서드 유지)
     */
    @GetMapping("/risk-students/all")
    public ResponseEntity<?> getAllRiskStudents() {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAllRiskStudents();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 위험 학생 조회 성공", results));
    }

    /**
     * 전체 학생 분석 결과 조회 (스태프용) - 모든 위험도 포함 (기존 메서드 유지)
     */
    @GetMapping("/students/all")
    public ResponseEntity<?> getAllStudents() {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAllStudents();
        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 학생 조회 성공", results));
    }

    // ===================== 페이징용 새 엔드포인트 =====================

    /**
     * 전체 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 학생 수 (기본 10명)
     * @param collegeId 단과대학 ID (선택)
     * @param departmentId 학과 ID (선택)
     * @param riskLevel 위험도 (선택: NORMAL, CAUTION, RISK, CRITICAL)
     */
    @GetMapping("/students/paged")
    public ResponseEntity<?> getAllStudentsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer collegeId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String riskLevel) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Map<String, Object>> results = aiAnalysisResultService.getAllStudentsGroupedByStudent(
                collegeId, departmentId, riskLevel, pageable);

        return ResponseEntity.ok(new AIResponseDTO<>(1, "전체 학생 페이징 조회 성공", results));
    }

    /**
     * 위험 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     * RISK, CRITICAL만 조회
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 학생 수 (기본 10명)
     * @param collegeId 단과대학 ID (선택)
     * @param departmentId 학과 ID (선택)
     * @param riskLevel 위험도 (선택: RISK, CRITICAL)
     * @param searchTerm 검색어 (학번, 이름, 학과명)
     */
    @GetMapping("/risk-students/paged")
    public ResponseEntity<?> getRiskStudentsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer collegeId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String riskLevel,
            @RequestParam(required = false) String searchTerm) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Map<String, Object>> results = aiAnalysisResultService.getRiskStudentsGroupedByStudent(
                collegeId, departmentId, riskLevel, searchTerm, pageable);

        return ResponseEntity.ok(new AIResponseDTO<>(1, "위험 학생 페이징 조회 성공", results));
    }

    // ===================== 기존 분석 실행 엔드포인트 (그대로 유지) =====================

    /**
     * AI 분석 실행
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeStudent(@RequestBody AnalyzeRequest request) {
        log.info("🎯 학생 분석: ID={}, 과목={}",
                request.getStudentId(), request.getSubjectId());

        long startTime = System.currentTimeMillis();

        AIAnalysisResult result = aiAnalysisResultService.analyzeStudent(
                request.getStudentId(),
                request.getSubjectId(),
                request.getYear(),
                request.getSemester()
        );

        long totalTime = System.currentTimeMillis() - startTime;

        Map<String, Object> response = new HashMap<>();
        response.put("analysisResult", result);
        response.put("processingTimeMs", totalTime);
        response.put("apiUsage", multiAIService.getAPIUsageStatus());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AIResponseDTO<>(
                        1,
                        String.format("AI 분석 완료 (소요: %dms)", totalTime),
                        response
                ));
    }

    /**
     * 전체 학생-과목 일괄 AI 분석 실행 (관리자용)
     */
    @PostMapping("/analyze-all")
    public ResponseEntity<?> analyzeAll(@RequestBody AnalyzeBatchRequest request) {
        log.info("🚀 일괄 분석 시작 (MultiAI 사용)");

        long startTime = System.currentTimeMillis();

        int count = aiAnalysisResultService.analyzeAllStudentsAndSubjects(
                request.getYear(),
                request.getSemester()
        );

        long totalTime = System.currentTimeMillis() - startTime;

        Map<String, Object> result = new HashMap<>();
        result.put("processedCount", count);
        result.put("totalTimeSeconds", totalTime / 1000);
        result.put("averageTimePerStudent", String.format("%.2f초", totalTime / 1000.0 / count));
        result.put("apiUsage", multiAIService.getAPIUsageStatus());

        return ResponseEntity.ok(new AIResponseDTO<>(
                1,
                String.format("✅ %d건 분석 완료 (소요: %d초)", count, totalTime / 1000),
                result
        ));
    }

    /**
     * 교수 담당 학생 분석 결과 조회 (교수용) - 모든 위험도 포함
     */
    @GetMapping("/advisor/{advisorId}/students")
    public ResponseEntity<?> getAdvisorStudents(@PathVariable Integer advisorId) {
        List<AIAnalysisResult> results = aiAnalysisResultService.getAdvisorStudents(advisorId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "담당 학생 조회 성공", results));
    }

    /**
     * 교수 담당 학생 분석 결과 조회 (페이징) - 학생별로 그룹핑
     * @param advisorId 교수 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지당 학생 수 (기본 10명)
     * @param riskLevel 위험도 (선택: NORMAL, CAUTION, RISK, CRITICAL)
     */
    @GetMapping("/advisor/{advisorId}/students/paged")
    public ResponseEntity<?> getAdvisorStudentsPaged(
            @PathVariable Integer advisorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String riskLevel) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Map<String, Object>> results = aiAnalysisResultService.getAdvisorStudentsGroupedByStudent(
                advisorId, riskLevel, pageable);

        return ResponseEntity.ok(new AIResponseDTO<>(1, "담당 학생 페이징 조회 성공", results));
    }

    /**
     * ✅ 새로운 엔드포인트: API 사용 상황 조회
     * Groq, Gemini 호출 횟수 및 남은 할당량 확인
     */
    @GetMapping("/api-status")
    public ResponseEntity<?> getAPIStatus() {
        Map<String, Object> status = multiAIService.getAPIUsageStatus();

        return ResponseEntity.ok(new AIResponseDTO<>(
                1,
                "API 사용 상황 조회 성공",
                status
        ));
    }



    // Request DTOs
    @lombok.Data
    static class AnalyzeBatchRequest {
        private Integer year;
        private Integer semester;
    }

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