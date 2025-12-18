package com.green.university.controller;

import com.green.university.dto.PersonalizedLearningDTO;
import com.green.university.service.PersonalizedLearningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 기반 맞춤형 학습 지원 컨트롤러
 * - 수동: 재분석 버튼으로 즉시 분석 가능
 * - 자동: 매달 1일 새벽 2시에 자동 분석 실행
 */
@Slf4j
@RestController
@RequestMapping("/api/personalized-learning")
@RequiredArgsConstructor
public class PersonalizedLearningController {

    private final PersonalizedLearningService personalizedLearningService;

    /**
     * 학생 맞춤형 학습 지원 데이터 조회 (최신 분석 결과 또는 새로 생성)
     *
     * @param studentId 학생 ID
     * @return 맞춤형 학습 지원 데이터
     */
    @GetMapping("/{studentId}")
    public ResponseEntity<PersonalizedLearningDTO> getPersonalizedLearning(
            @PathVariable Integer studentId) {

        log.info("맞춤형 학습 지원 요청 - 학생 ID: {}", studentId);

        try {
            PersonalizedLearningDTO result = personalizedLearningService.getPersonalizedLearning(studentId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("학생을 찾을 수 없음: {}", studentId, e);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("맞춤형 학습 지원 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 강제로 새로운 분석 생성 (기존 데이터 무시하고 재생성)
     * 수동 재분석 버튼에서 사용
     *
     * @param studentId 학생 ID
     * @return 새로 생성된 맞춤형 학습 지원 데이터
     */
    @PostMapping("/{studentId}/regenerate")
    public ResponseEntity<PersonalizedLearningDTO> regenerateAnalysis(
            @PathVariable Integer studentId) {

        log.info("분석 결과 재생성 요청 - 학생 ID: {}", studentId);

        try {
            PersonalizedLearningDTO result = personalizedLearningService.regenerateAnalysis(studentId);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("학생을 찾을 수 없음: {}", studentId, e);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("분석 결과 재생성 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 특정 학기의 분석 결과 조회
     *
     * @param studentId 학생 ID
     * @param year 연도
     * @param semester 학기 (1 또는 2)
     * @return 특정 학기의 분석 결과
     */
    @GetMapping("/{studentId}/period")
    public ResponseEntity<PersonalizedLearningDTO> getAnalysisByPeriod(
            @PathVariable Integer studentId,
            @RequestParam Integer year,
            @RequestParam Integer semester) {

        log.info("특정 학기 분석 결과 조회 - 학생 ID: {}, {}년 {}학기", studentId, year, semester);

        try {
            PersonalizedLearningDTO result =
                    personalizedLearningService.getAnalysisByPeriod(studentId, year, semester);
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            log.error("분석 결과를 찾을 수 없음: {}년 {}학기", year, semester, e);
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            log.error("특정 학기 분석 결과 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 학생의 전체 분석 이력 조회
     *
     * @param studentId 학생 ID
     * @return 전체 분석 이력 (최신순)
     */
    @GetMapping("/{studentId}/history")
    public ResponseEntity<List<PersonalizedLearningDTO>> getAnalysisHistory(
            @PathVariable Integer studentId) {

        log.info("전체 분석 이력 조회 - 학생 ID: {}", studentId);

        try {
            List<PersonalizedLearningDTO> history =
                    personalizedLearningService.getAnalysisHistory(studentId);
            return ResponseEntity.ok(history);

        } catch (Exception e) {
            log.error("분석 이력 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 학습 이력 분석만 조회 (최신 분석 기준)
     */
    @GetMapping("/{studentId}/learning-history")
    public ResponseEntity<?> getLearningHistory(@PathVariable Integer studentId) {
        try {
            PersonalizedLearningDTO result = personalizedLearningService.getPersonalizedLearning(studentId);
            return ResponseEntity.ok(result.getLearningHistory());
        } catch (Exception e) {
            log.error("학습 이력 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 추천 과목만 조회 (최신 분석 기준)
     */
    @GetMapping("/{studentId}/recommendations")
    public ResponseEntity<?> getRecommendations(@PathVariable Integer studentId) {
        try {
            PersonalizedLearningDTO result = personalizedLearningService.getPersonalizedLearning(studentId);
            return ResponseEntity.ok(Map.of(
                    "majors", result.getRecommendedMajors(),
                    "electives", result.getRecommendedElectives()
            ));
        } catch (Exception e) {
            log.error("추천 과목 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 졸업 요건 분석만 조회 (최신 분석 기준)
     */
    @GetMapping("/{studentId}/graduation")
    public ResponseEntity<?> getGraduationRequirement(@PathVariable Integer studentId) {
        try {
            PersonalizedLearningDTO result = personalizedLearningService.getPersonalizedLearning(studentId);
            return ResponseEntity.ok(result.getGraduationRequirement());
        } catch (Exception e) {
            log.error("졸업 요건 조회 실패", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}