package com.green.university.scheduler;

import com.green.university.service.AIAnalysisResultService;
import com.green.university.utils.Define;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AI 분석 정기 배치 스케줄러
 *
 * - 매일 새벽 2시: 전체 학생 AI 분석 재실행
 * - 매주 월요일 오전 3시: 위험 학생 요약 리포트 (선택)
 */
@Component
@RequiredArgsConstructor
public class AIAnalysisScheduler {

    private final AIAnalysisResultService aiAnalysisResultService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 매일 새벽 2시에 전체 학생 AI 분석 실행
     * cron 표현식: 초 분 시 일 월 요일
     * "0 0 2 * * *" = 매일 02:00:00
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void scheduleDailyAIAnalysis() {
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("====================================");
        System.out.println("🤖 정기 AI 분석 배치 시작");
        System.out.println("시작 시간: " + startTime.format(FORMATTER));
        System.out.println("대상: " + Define.CURRENT_YEAR + "년 " + Define.CURRENT_SEMESTER + "학기");
        System.out.println("====================================");

        try {
            // 전체 학생-과목 AI 분석 실행
            int count = aiAnalysisResultService.analyzeAllStudentsAndSubjects(
                    Define.CURRENT_YEAR,
                    Define.CURRENT_SEMESTER
            );

            LocalDateTime endTime = LocalDateTime.now();
            long duration = java.time.Duration.between(startTime, endTime).getSeconds();

            System.out.println("====================================");
            System.out.println("✅ 정기 AI 분석 배치 완료");
            System.out.println("처리 건수: " + count + "건");
            System.out.println("소요 시간: " + duration + "초");
            System.out.println("종료 시간: " + endTime.format(FORMATTER));
            System.out.println("====================================");

        } catch (Exception e) {
            LocalDateTime errorTime = LocalDateTime.now();
            System.err.println("====================================");
            System.err.println("❌ 정기 AI 분석 배치 실패");
            System.err.println("실패 시간: " + errorTime.format(FORMATTER));
            System.err.println("오류 메시지: " + e.getMessage());
            System.err.println("====================================");
            e.printStackTrace();
        }
    }

    /**
     * 매주 월요일 오전 3시에 위험 학생 요약 (선택사항)
     * "0 0 3 * * MON" = 매주 월요일 03:00:00
     */
    @Scheduled(cron = "0 0 3 * * MON")
    public void scheduleWeeklyRiskSummary() {
        LocalDateTime now = LocalDateTime.now();
        System.out.println("====================================");
        System.out.println("📊 주간 위험 학생 요약");
        System.out.println("실행 시간: " + now.format(FORMATTER));
        System.out.println("====================================");

        try {
            // 위험 학생 통계 조회
            var riskStudents = aiAnalysisResultService.getAllRiskStudents();

            long criticalCount = riskStudents.stream()
                    .filter(r -> "CRITICAL".equals(r.getOverallRisk()))
                    .count();

            long riskCount = riskStudents.stream()
                    .filter(r -> "RISK".equals(r.getOverallRisk()))
                    .count();

            long cautionCount = riskStudents.stream()
                    .filter(r -> "CAUTION".equals(r.getOverallRisk()))
                    .count();

            System.out.println("🔴 심각(CRITICAL): " + criticalCount + "명");
            System.out.println("🟠 위험(RISK): " + riskCount + "명");
            System.out.println("🟡 주의(CAUTION): " + cautionCount + "명");
            System.out.println("📈 총 위험 학생: " + riskStudents.size() + "명");
            System.out.println("====================================");

            // TODO: 이메일 알림, Slack 알림 등 추가 가능

        } catch (Exception e) {
            System.err.println("❌ 주간 요약 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }


}