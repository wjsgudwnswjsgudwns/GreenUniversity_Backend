package com.green.university.service;

import com.green.university.repository.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAIAnalysisService {

    private final AIAnalysisResultService aiAnalysisResultService;

    /**
     * 비동기 병렬 분석 (속도 개선)
     * 10명씩 배치로 처리하여 Rate Limit 회피
     */
    @Async
    @Transactional
    public CompletableFuture<Integer> analyzeAllStudentsAsync(
            List<StuSubDetail> enrollments,
            Integer year,
            Integer semester) {

        int batchSize = 10; // 10명씩 배치 처리
        int totalProcessed = 0;

        log.info("🚀 비동기 분석 시작: 총 {}건", enrollments.size());

        for (int i = 0; i < enrollments.size(); i += batchSize) {
            int end = Math.min(i + batchSize, enrollments.size());
            List<StuSubDetail> batch = enrollments.subList(i, end);

            log.info("📦 배치 {}/{} 처리 중 ({}-{}번)",
                    (i / batchSize) + 1,
                    (enrollments.size() + batchSize - 1) / batchSize,
                    i + 1, end);

            // 배치 내에서는 병렬 처리
            List<CompletableFuture<Void>> futures = batch.stream()
                    .map(enrollment -> CompletableFuture.runAsync(() -> {
                        try {
                            aiAnalysisResultService.analyzeStudent(
                                    enrollment.getStudentId(),
                                    enrollment.getSubjectId(),
                                    year != null ? year :
                                            (enrollment.getSubject() != null ?
                                                    enrollment.getSubject().getSubYear() : null),
                                    semester != null ? semester :
                                            (enrollment.getSubject() != null ?
                                                    enrollment.getSubject().getSemester() : null)
                            );
                        } catch (Exception e) {
                            log.error("분석 실패: 학생={}, 과목={}, 에러={}",
                                    enrollment.getStudentId(),
                                    enrollment.getSubjectId(),
                                    e.getMessage());
                        }
                    }))
                    .collect(Collectors.toList());

            // 배치 완료 대기
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .join();

            totalProcessed += batch.size();

            // 배치 간 Rate Limit 방지 대기 (5초)
            if (end < enrollments.size()) {
                try {
                    log.info("⏱️ Rate Limit 방지 대기 (5초)...");
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("✅ 비동기 분석 완료: {}건 처리", totalProcessed);
        return CompletableFuture.completedFuture(totalProcessed);
    }
}