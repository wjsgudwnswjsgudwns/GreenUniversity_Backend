package com.green.university.controller;

import com.green.university.dto.AIResponseDTO;
import com.green.university.repository.model.AICounseling;
import com.green.university.service.AICounselingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ai-counseling")
@RequiredArgsConstructor
public class AICounselingController {

    private final AICounselingService aiCounselingService;

    /**
     * 학생의 상담 일정 조회
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<?> getStudentCounselings(@PathVariable Integer studentId) {
        List<AICounseling> counselings = aiCounselingService.getStudentCounselings(studentId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "학생 상담 일정 조회 성공", counselings));
    }

    /**
     * 학생의 예정된 상담 일정 조회 (미완료만)
     */
    @GetMapping("/student/{studentId}/upcoming")
    public ResponseEntity<?> getUpcomingCounselings(@PathVariable Integer studentId) {
        List<AICounseling> counselings = aiCounselingService.getUpcomingCounselings(studentId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "예정된 상담 일정 조회 성공", counselings));
    }

    /**
     * 교수의 상담 일정 조회
     */
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<?> getProfessorCounselings(@PathVariable Integer professorId) {
        List<AICounseling> counselings = aiCounselingService.getProfessorCounselings(professorId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "교수 상담 일정 조회 성공", counselings));
    }

    /**
     * 과목별 상담 일정 조회
     */
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<?> getSubjectCounselings(@PathVariable Integer subjectId) {
        List<AICounseling> counselings = aiCounselingService.getSubjectCounselings(subjectId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "과목별 상담 일정 조회 성공", counselings));
    }

    /**
     * 교수-학생 간 상담 내역 조회
     */
    @GetMapping("/professor/{professorId}/student/{studentId}")
    public ResponseEntity<?> getCounselingsByProfessorAndStudent(
            @PathVariable Integer professorId,
            @PathVariable Integer studentId) {
        List<AICounseling> counselings = aiCounselingService.getCounselingsByProfessorAndStudent(professorId, studentId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "교수-학생 상담 내역 조회 성공", counselings));
    }

    /**
     * 상담 일정 생성
     */
    @PostMapping
    public ResponseEntity<?> createCounseling(@RequestBody AICounseling counseling) {
        AICounseling created = aiCounselingService.createCounseling(counseling);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AIResponseDTO<>(1, "상담 일정 생성 성공", created));
    }

    /**
     * 상담 기록 + Gemini AI 분석
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> createCounselingWithAnalysis(@RequestBody CreateCounselingWithAnalysisRequest request) {
        AICounseling counseling = aiCounselingService.createCounselingWithAnalysis(
                request.getStudentId(),
                request.getProfessorId(),
                request.getSubjectId(),
                request.getScheduledAt(),
                request.getCounselingContent()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AIResponseDTO<>(1, "상담 분석 완료", counseling));
    }

    /**
     * 상담 내용 작성 및 완료 처리
     */
    @PutMapping("/{counselingId}/complete")
    public ResponseEntity<?> completeCounseling(
            @PathVariable Integer counselingId,
            @RequestBody CompleteCounselingRequest request) {
        AICounseling completed = aiCounselingService.completeCounseling(counselingId, request.getCounselingContent());
        return ResponseEntity.ok(new AIResponseDTO<>(1, "상담 완료 처리 성공", completed));
    }

    /**
     * 상담 일정 수정
     */
    @PutMapping("/{counselingId}")
    public ResponseEntity<?> updateCounseling(
            @PathVariable Integer counselingId,
            @RequestBody UpdateCounselingRequest request) {
        AICounseling updated = aiCounselingService.updateCounseling(counselingId, request.getScheduledAt());
        return ResponseEntity.ok(new AIResponseDTO<>(1, "상담 일정 수정 성공", updated));
    }

    /**
     * 상담 일정 삭제
     */
    @DeleteMapping("/{counselingId}")
    public ResponseEntity<?> deleteCounseling(@PathVariable Integer counselingId) {
        aiCounselingService.deleteCounseling(counselingId);
        return ResponseEntity.ok(new AIResponseDTO<>(1, "상담 일정 삭제 성공", null));
    }

    // Request DTOs
    @lombok.Data
    static class CreateCounselingWithAnalysisRequest {
        private Integer studentId;
        private Integer professorId;
        private Integer subjectId;
        private LocalDateTime scheduledAt;
        private String counselingContent;
    }

    @lombok.Data
    static class CompleteCounselingRequest {
        private String counselingContent;
    }

    @lombok.Data
    static class UpdateCounselingRequest {
        private LocalDateTime scheduledAt;
    }
}