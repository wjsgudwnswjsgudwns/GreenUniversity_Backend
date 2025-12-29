package com.green.university.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.StuSubDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 여러 무료 AI API를 번갈아 사용하여 Rate Limit 회피
 *
 * 지원 AI:
 * 1. Gemini 2.5 Flash (분당 15개)
 * 2. Groq Llama 3.3 70B (분당 30개)
 * 전략: Groq 우선 → Rate Limit 시 Gemini → 둘 다 실패 시 규칙 기반
 */
@Slf4j
@Service
public class MultiAIService {

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Value("${groq.api.key:}")
    private String groqKey;

    @Value("${together.api.key:}")
    private String togetherKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private int groqCallCount = 0;
    private int geminiCallCount = 0;
    private long lastResetTime = System.currentTimeMillis();

    /**
     * 중도이탈 위험 종합 예측 (여러 AI 시도)
     */
    public String predictOverallDropoutRisk(AIAnalysisResult result, StuSubDetail detail) {
        resetCountersIfNeeded();

        // 데이터 준비
        int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
        int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
        int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
        int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
        int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;

        // 각 항목별 상태
        String attendanceStatus = result.getAttendanceStatus() != null ? result.getAttendanceStatus() : "NORMAL";
        String homeworkStatus = result.getHomeworkStatus() != null ? result.getHomeworkStatus() : "NORMAL";
        String midtermStatus = result.getMidtermStatus() != null ? result.getMidtermStatus() : "NORMAL";
        String finalStatus = result.getFinalStatus() != null ? result.getFinalStatus() : "NORMAL";
        String tuitionStatus = result.getTuitionStatus() != null ? result.getTuitionStatus() : "NORMAL";
        String counselingStatus = result.getCounselingStatus() != null ? result.getCounselingStatus() : "NORMAL";

        // ⭐ 개선된 프롬프트
        String prompt = String.format(
                "당신은 대학생 중도이탈 위험 예측 전문 AI입니다. 다음 학생 데이터를 종합 분석하여 중도이탈 위험도를 정확히 판단해주세요.\n\n" +

                        "=== 학생 학업 데이터 ===\n" +
                        "📊 출석 상황: %s\n" +
                        "   - 결석: %d회\n" +
                        "   - 지각: %d회\n" +
                        "   - 환산 결석: %.1f회 (지각 3회 = 결석 1회)\n" +
                        "   - ⚠️ 환산 결석 3회 이상 = F학점 자동 부여\n\n" +

                        "📝 과제: %s (%d점/100점)\n\n" +

                        "📖 중간고사: %s (%d점/100점)\n\n" +

                        "📖 기말고사: %s (%d점/100점)\n" +
                        "   - 시험 평균: %.1f점\n\n" +

                        "💰 등록금: %s\n" +
                        "   - NORMAL: 납부 완료\n" +
                        "   - CAUTION: 미납 (경제적 어려움 가능성)\n\n" +

                        "🗣️ 상담: %s\n" +
                        "   - 이미 상담 내용을 AI가 분석한 결과\n\n" +

                        "=== 중요한 판단 원칙 ===\n\n" +

                        "**1. 학업 데이터 우선 (가중치 높음)**\n" +
                        "   - 출석, 과제, 시험 성적이 중도이탈의 직접적 지표\n" +
                        "   - 이들 중 하나라도 심각하면 CRITICAL 고려\n\n" +

                        "**2. 등록금은 보조 지표 (가중치 낮음)**\n" +
                        "   - 등록금 미납만으로는 CRITICAL 아님\n" +
                        "   - 학업이 정상이면 등록금 미납은 CAUTION 정도\n" +
                        "   - 학업 + 등록금 둘 다 문제면 가중\n\n" +

                        "**3. 복합 평가**\n" +
                        "   - 여러 문제 동시 발생 시 위험도 상승\n" +
                        "   - 단일 문제는 그 심각도에 맞게 판단\n\n" +

                        "=== 위험도 판단 기준 (엄격 적용!) ===\n\n" +

                        "**CRITICAL** (매우 위험 - 즉각 개입):\n" +
                        "• **학업 CRITICAL + 다른 문제**: 출석/과제/시험 중 하나가 CRITICAL이고 다른 항목도 문제\n" +
                        "• **F학점 확정**: 환산 결석 3회 이상\n" +
                        "• **학업 완전 포기**: 시험 평균 30점 미만\n" +
                        "• **복합 위기**: RISK 3개 이상 동시 발생\n" +
                        "• **상담 CRITICAL**: 심각한 심리/정신 문제\n\n" +

                        "**RISK** (위험 - 집중 관리):\n" +
                        "• **학업 CRITICAL 1개**: 출석/과제/시험 중 하나만 CRITICAL\n" +
                        "• **학업 RISK 2개 이상**: 여러 학업 영역에서 문제\n" +
                        "• **F학점 위험**: 환산 결석 2~2.9회\n" +
                        "• **학업 + 등록금**: 학업 문제 + 등록금 미납\n\n" +

                        "**CAUTION** (주의 - 모니터링):\n" +
                        "• **단일 RISK**: 한 영역만 RISK\n" +
                        "• **여러 CAUTION**: 2-3개 영역이 CAUTION\n" +
                        "• **등록금만 문제**: 학업 정상, 등록금만 미납\n" +
                        "• **경미한 학업 부진**: 시험 50-65점, 결석 1-2회\n\n" +

                        "**NORMAL** (정상):\n" +
                        "• **대부분 정상**: 5개 이상 항목 NORMAL\n" +
                        "• **경미한 문제**: CAUTION 1개 이하\n" +
                        "• **학업 양호**: 출석 90%% 이상, 시험 65점 이상\n\n" +

                        "=== 예시 케이스 ===\n\n" +

                        "**예시 1: CRITICAL**\n" +
                        "- 출석: CRITICAL (결석 4회) ← F학점 확정\n" +
                        "- 과제: NORMAL\n" +
                        "- 시험: CAUTION\n" +
                        "→ 결과: CRITICAL (F학점 확정이므로)\n\n" +

                        "**예시 2: CAUTION (CRITICAL 아님!)**\n" +
                        "- 출석: NORMAL\n" +
                        "- 과제: NORMAL\n" +
                        "- 시험: NORMAL\n" +
                        "- 등록금: CAUTION (미납)\n" +
                        "→ 결과: CAUTION (등록금만 문제, 학업 정상)\n\n" +

                        "**예시 3: RISK**\n" +
                        "- 출석: RISK (결석 2회)\n" +
                        "- 과제: RISK (50점)\n" +
                        "- 시험: NORMAL\n" +
                        "→ 결과: RISK (2개 영역 RISK)\n\n" +

                        "=== 출력 형식 (절대 준수!) ===\n" +
                        "위 데이터를 종합 분석하여 **정확히 한 단어만** 응답하세요:\n" +
                        "CRITICAL, RISK, CAUTION, NORMAL\n\n" +

                        "다른 설명이나 부가 텍스트 없이 위험도 단어 하나만 출력하세요.",

                // 상태값 전달
                attendanceStatus, absent, lateness, absent + (lateness / 3.0),
                homeworkStatus, homework,
                midtermStatus, midExam,
                finalStatus, finalExam, (midExam + finalExam) / 2.0,
                tuitionStatus,
                counselingStatus
        );

        // AI 호출 로직 (기존과 동일)
        if (groqKey != null && !groqKey.isEmpty() && groqCallCount < 25) {
            try {
                log.info("🟢 Groq API 시도 (호출 {}/25)", groqCallCount + 1);
                String result1 = callGroqAPI(prompt);
                groqCallCount++;
                return parseRiskLevel(result1);
            } catch (Exception e) {
                log.warn("⚠️ Groq 실패, Gemini로 전환: {}", e.getMessage());
            }
        }

        if (geminiCallCount < 12) {
            try {
                log.info("🔵 Gemini API 시도 (호출 {}/12)", geminiCallCount + 1);
                String result2 = callGeminiAPI(prompt);
                geminiCallCount++;
                return parseRiskLevel(result2);
            } catch (Exception e) {
                log.error("❌ Gemini 실패: {}", e.getMessage());
            }
        }

        log.error("❌ 모든 AI API 실패");
        return null;
    }

    /**
     * 위험 코멘트 생성 (여러 AI 시도)
     */
    public String generateRiskComment(AIAnalysisResult result, StuSubDetail detail) {
        resetCountersIfNeeded();

        if (!"RISK".equals(result.getOverallRisk()) && !"CRITICAL".equals(result.getOverallRisk())) {
            return null;
        }

        StringBuilder dataBuilder = new StringBuilder();
        dataBuilder.append("=== 학생 데이터 분석 ===\n\n");

        if (!"NORMAL".equals(result.getAttendanceStatus())) {
            int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
            int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
            dataBuilder.append(String.format("📌 출석 상태: %s\n", result.getAttendanceStatus()));
            dataBuilder.append(String.format("   - 결석: %d회, 지각: %d회\n", absent, lateness));
        }

        if (!"NORMAL".equals(result.getHomeworkStatus())) {
            int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
            dataBuilder.append(String.format("📌 과제 상태: %s\n", result.getHomeworkStatus()));
            dataBuilder.append(String.format("   - 과제 점수: %d점\n", homework));
        }

        if (!"NORMAL".equals(result.getMidtermStatus())) {
            int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
            dataBuilder.append(String.format("📌 중간고사 상태: %s\n", result.getMidtermStatus()));
            dataBuilder.append(String.format("   - 중간고사 점수: %d점\n", midExam));
        }

        if (!"NORMAL".equals(result.getFinalStatus())) {
            int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;
            dataBuilder.append(String.format("📌 기말고사 상태: %s\n", result.getFinalStatus()));
            dataBuilder.append(String.format("   - 기말고사 점수: %d점\n", finalExam));
        }

        if (!"NORMAL".equals(result.getTuitionStatus())) {
            dataBuilder.append(String.format("📌 등록금 상태: %s\n", result.getTuitionStatus()));
            dataBuilder.append("   - 등록금 미납 상태\n");
        }

        if (!"NORMAL".equals(result.getCounselingStatus()) && result.getCounselingStatus() != null) {
            dataBuilder.append(String.format("📌 상담 상태: %s\n", result.getCounselingStatus()));
            dataBuilder.append("   - 상담 내용에서 위험 신호 감지\n");
        }

        String prompt = String.format(
                "당신은 대학생 학업 지원 전문가입니다. 다음 학생 데이터를 분석하여 중도 이탈 위험의 주요 원인을 **간결하고 명확하게** 설명해주세요.\n\n" +
                        "%s\n\n" +
                        "=== 분석 요구사항 ===\n" +
                        "1. **2-3문장으로 핵심만 간결하게** 작성하세요\n" +
                        "2. 가장 심각한 문제부터 우선순위로 언급하세요\n" +
                        "3. 구체적인 수치를 언급하며 설명하세요\n" +
                        "4. 교육적이고 객관적인 톤을 유지하세요\n" +
                        "5. 불필요한 인사말이나 서론 없이 바로 분석 내용으로 시작하세요\n\n" +
                        "위 형식으로 이 학생의 위험 요인을 분석해주세요:",
                dataBuilder.toString()
        );

        // 1순위: Groq
        if (groqKey != null && !groqKey.isEmpty() && groqCallCount < 25) {
            try {
                log.info("🟢 Groq API 코멘트 생성 (호출 {}/25)", groqCallCount + 1);
                String comment = callGroqAPI(prompt);
                groqCallCount++;
                return comment.trim();
            } catch (Exception e) {
                log.warn("⚠️ Groq 코멘트 실패, Gemini로 전환");
            }
        }

        // 2순위: Gemini
        if (geminiCallCount < 12) {
            try {
                log.info("🔵 Gemini API 코멘트 생성 (호출 {}/12)", geminiCallCount + 1);
                String comment = callGeminiAPI(prompt);
                geminiCallCount++;
                return comment.trim();
            } catch (Exception e) {
                log.warn("⚠️ Gemini 코멘트 실패");
            }
        }

        log.warn("⚠️ AI 코멘트 생성 실패, 기본 코멘트 사용");
        return generateFallbackComment(result, detail);
    }

    /**
     * Groq API 호출 (Llama 3.3 70B)
     * 무료 tier: 분당 30개, 일일 14,400개
     */
    private String callGroqAPI(String prompt) throws Exception {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llama-3.3-70b-versatile");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 100);
        requestBody.put("top_p", 1);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");

        if (choices.isArray() && choices.size() > 0) {
            String content = choices.get(0).path("message").path("content").asText();
            log.info("✅ Groq 응답: {}", content.substring(0, Math.min(50, content.length())));
            return content;
        }

        throw new Exception("Groq 응답 파싱 실패");
    }

    /**
     * Gemini API 호출 (기존)
     */
    private String callGeminiAPI(String prompt) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash-exp:generateContent?key=" + geminiKey;

        Map<String, Object> requestBody = new HashMap<>();
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> part = new HashMap<>();
        part.put("text", prompt);
        parts.add(part);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode candidates = root.path("candidates");

        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode firstCandidate = candidates.get(0);
            JsonNode contentNode = firstCandidate.path("content");
            JsonNode partsNode = contentNode.path("parts");

            if (partsNode.isArray() && partsNode.size() > 0) {
                String text = partsNode.get(0).path("text").asText();
                log.info("✅ Gemini 응답: {}", text.substring(0, Math.min(50, text.length())));
                return text.trim();
            }
        }

        throw new Exception("Gemini 응답 파싱 실패");
    }

    /**
     * Together AI 호출 (옵션)
     * 무료 tier: 분당 60개
     */
    private String callTogetherAPI(String prompt) throws Exception {
        String url = "https://api.together.xyz/v1/chat/completions";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "meta-llama/Llama-3.3-70B-Instruct-Turbo");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "user", "content", prompt));
        requestBody.put("messages", messages);

        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 100);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + togetherKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.path("choices");

        if (choices.isArray() && choices.size() > 0) {
            String content = choices.get(0).path("message").path("content").asText();
            log.info("✅ Together AI 응답: {}", content.substring(0, Math.min(50, content.length())));
            return content;
        }

        throw new Exception("Together AI 응답 파싱 실패");
    }

    /**
     * 위험도 파싱
     */
    private String parseRiskLevel(String response) {
        if (response == null || response.isEmpty()) {
            log.warn("⚠️ 응답이 비어있음");
            return null;
        }

        String upperResponse = response.toUpperCase().trim();

        if (upperResponse.equals("CRITICAL")) return "CRITICAL";
        if (upperResponse.equals("RISK")) return "RISK";
        if (upperResponse.equals("CAUTION")) return "CAUTION";
        if (upperResponse.equals("NORMAL")) return "NORMAL";

        if (upperResponse.contains("CRITICAL")) return "CRITICAL";
        if (upperResponse.contains("RISK")) return "RISK";
        if (upperResponse.contains("CAUTION")) return "CAUTION";
        if (upperResponse.contains("NORMAL")) return "NORMAL";

        log.error("❌ 유효하지 않은 위험도 응답: {}", response);
        return null;
    }

    /**
     * AI 코멘트 생성 실패 시 폴백
     */
    private String generateFallbackComment(AIAnalysisResult result, StuSubDetail detail) {
        StringBuilder comment = new StringBuilder();
        List<String> issues = new ArrayList<>();

        if (!"NORMAL".equals(result.getAttendanceStatus())) {
            int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
            int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
            issues.add(String.format("출석 문제 (결석 %d회, 지각 %d회)", absent, lateness));
        }

        if (!"NORMAL".equals(result.getHomeworkStatus())) {
            int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
            issues.add(String.format("과제 미흡 (%d점)", homework));
        }

        if (!"NORMAL".equals(result.getMidtermStatus())) {
            int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
            issues.add(String.format("중간고사 저조 (%d점)", midExam));
        }

        if (!"NORMAL".equals(result.getFinalStatus())) {
            int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;
            issues.add(String.format("기말고사 저조 (%d점)", finalExam));
        }

        if (!"NORMAL".equals(result.getTuitionStatus())) {
            issues.add("등록금 미납");
        }

        if (!"NORMAL".equals(result.getCounselingStatus())) {
            issues.add("상담 내용에서 위험 신호 감지");
        }

        if (issues.isEmpty()) {
            return "모니터링이 필요한 학생입니다.";
        }

        comment.append("다음 영역에서 문제가 감지되었습니다: ");
        comment.append(String.join(", ", issues));
        comment.append(". 즉각적인 학습 지원과 상담이 필요합니다.");

        return comment.toString();
    }

    /**
     * 호출 카운터 초기화 (1분마다)
     */
    private void resetCountersIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastResetTime > 60000) {
            log.info("🔄 API 호출 카운터 초기화 (Groq: {}, Gemini: {})",
                    groqCallCount, geminiCallCount);
            groqCallCount = 0;
            geminiCallCount = 0;
            lastResetTime = now;
        }
    }

    /**
     * 현재 API 사용 상황 조회
     */
    public Map<String, Object> getAPIUsageStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("groqCallCount", groqCallCount);
        status.put("groqLimit", 25);
        status.put("groqAvailable", groqKey != null && !groqKey.isEmpty());

        status.put("geminiCallCount", geminiCallCount);
        status.put("geminiLimit", 12);
        status.put("geminiAvailable", geminiKey != null && !geminiKey.isEmpty());

        status.put("togetherAvailable", togetherKey != null && !togetherKey.isEmpty());

        long timeUntilReset = 60000 - (System.currentTimeMillis() - lastResetTime);
        status.put("timeUntilResetSeconds", timeUntilReset / 1000);

        return status;
    }
}