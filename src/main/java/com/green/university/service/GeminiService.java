package com.green.university.service;

import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.StuSubDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 상담 내용을 Gemini AI로 분석하여 위험도 반환
     */
    public String analyzeCounselingContent(String counselingContent) {
        try {
            String prompt = String.format(
                    "당신은 대학 학생 상담 전문가입니다. 다음 상담 내용을 신중히 분석하여 학생의 중도이탈 위험도를 판단해주세요.\n\n" +
                            "=== 상담 내용 ===\n%s\n\n" +
                            "=== 분석 가이드라인 ===\n" +
                            "1. 문제가 **현재 진행 중**인지, **이미 해결**되었는지 구분하세요.\n" +
                            "2. 긍정적인 변화(문제 해결, 개선)는 위험도를 낮춥니다.\n" +
                            "3. 단순 언급과 실제 심각한 문제를 구분하세요.\n" +
                            "4. 학생의 태도와 의지를 고려하세요.\n\n" +
                            "=== 위험도 판단 기준 ===\n" +
                            "**CRITICAL** (매우 심각):\n" +
                            "- 자퇴/휴학을 심각하게 고민 중\n" +
                            "- 심각한 정신건강 문제(우울증, 자해 충동 등)\n" +
                            "- 해결되지 않은 심각한 경제적 어려움(등록금 미납, 생계 곤란)\n" +
                            "- 학업 포기 의사 표현\n" +
                            "- 지속적이고 심각한 가정 문제\n\n" +
                            "**RISK** (위험):\n" +
                            "- 지속적인 학업 부진과 무기력\n" +
                            "- 반복되는 결석/지각\n" +
                            "- 진로에 대한 심각한 회의감\n" +
                            "- 학교 생활 적응 실패\n" +
                            "- 해결되지 않은 경제적 어려움(장학금 탈락 등)\n\n" +
                            "**CAUTION** (주의):\n" +
                            "- 일시적 학업 부진\n" +
                            "- 경미한 적응 문제\n" +
                            "- 과거에 있었으나 현재는 개선된 문제\n" +
                            "- 작은 고민이나 스트레스\n\n" +
                            "**NORMAL** (정상):\n" +
                            "- 일반적인 진로 상담\n" +
                            "- 수강 신청 관련 상담\n" +
                            "- 문제가 해결되었거나 긍정적인 상태\n" +
                            "- 성적 향상, 동기 부여 등 긍정적 변화\n" +
                            "- 단순 정보 문의\n\n" +
                            "위 기준을 바탕으로 상담 내용을 분석한 후, 반드시 다음 4가지 중 **정확히 하나**만 응답하세요:\n" +
                            "CRITICAL, RISK, CAUTION, NORMAL\n\n" +
                            "다른 설명이나 부가 텍스트 없이 위험도 단어 하나만 출력하세요.",
                    counselingContent
            );

            String geminiResponse = callGeminiApi(prompt);
            return parseRiskLevel(geminiResponse);

        } catch (Exception e) {
            System.err.println("Gemini 분석 실패: " + e.getMessage());
            e.printStackTrace();
            return "NORMAL";
        }
    }

    /**
     * 학생의 종합 데이터를 분석하여 위험 원인 코멘트 생성
     */
    public String generateRiskComment(AIAnalysisResult result, StuSubDetail detail) {
        try {
            // 위험도가 RISK 또는 CRITICAL인 경우에만 상세 분석
            if (!"RISK".equals(result.getOverallRisk()) && !"CRITICAL".equals(result.getOverallRisk())) {
                return null;
            }

            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append("=== 학생 데이터 분석 ===\n\n");

            // 출석 상태
            if (!"NORMAL".equals(result.getAttendanceStatus())) {
                int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
                int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
                dataBuilder.append(String.format("📌 출석 상태: %s\n", result.getAttendanceStatus()));
                dataBuilder.append(String.format("   - 결석: %d회, 지각: %d회\n", absent, lateness));
            }

            // 과제 상태
            if (!"NORMAL".equals(result.getHomeworkStatus())) {
                int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
                dataBuilder.append(String.format("📌 과제 상태: %s\n", result.getHomeworkStatus()));
                dataBuilder.append(String.format("   - 과제 점수: %d점\n", homework));
            }

            // 중간고사 상태
            if (!"NORMAL".equals(result.getMidtermStatus())) {
                int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
                dataBuilder.append(String.format("📌 중간고사 상태: %s\n", result.getMidtermStatus()));
                dataBuilder.append(String.format("   - 중간고사 점수: %d점\n", midExam));
            }

            // 기말고사 상태
            if (!"NORMAL".equals(result.getFinalStatus())) {
                int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;
                dataBuilder.append(String.format("📌 기말고사 상태: %s\n", result.getFinalStatus()));
                dataBuilder.append(String.format("   - 기말고사 점수: %d점\n", finalExam));
            }

            // 등록금 상태
            if (!"NORMAL".equals(result.getTuitionStatus())) {
                dataBuilder.append(String.format("📌 등록금 상태: %s\n", result.getTuitionStatus()));
                dataBuilder.append("   - 등록금 미납 상태\n");
            }

            // 상담 상태
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
                            "=== 예시 ===\n" +
                            "\"결석 3회와 지각 6회로 출석률이 심각하게 낮으며, 과제 점수 35점으로 학업 수행도가 매우 부진합니다. 중간고사 28점으로 학업 이해도가 낮아 즉각적인 학습 지원이 필요합니다.\"\n\n" +
                            "위 형식으로 이 학생의 위험 요인을 분석해주세요:",
                    dataBuilder.toString()
            );

            String comment = callGeminiApi(prompt);
            return comment.trim();

        } catch (Exception e) {
            System.err.println("AI 코멘트 생성 실패: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gemini API 호출 - 개선된 에러 처리
     */
    private String callGeminiApi(String prompt) {
        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + apiKey;

                // Request Body 구성
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

                // HTTP Headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                // API 호출
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                // 응답 파싱
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");

                if (candidates.isArray() && candidates.size() > 0) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode contentNode = firstCandidate.path("content");
                    JsonNode partsNode = contentNode.path("parts");

                    if (partsNode.isArray() && partsNode.size() > 0) {
                        String text = partsNode.get(0).path("text").asText();
                        return text.trim();
                    }
                }

                log.warn("⚠️ Gemini 응답에 유효한 내용이 없음");
                return null;

            } catch (HttpClientErrorException.TooManyRequests e) {
                retryCount++;

                if (retryCount >= maxRetries) {
                    log.error("❌ Gemini API 할당량 초과, 최대 재시도 횟수 도달");
                    return null; // 실패 시 null 반환하여 폴백 로직 사용
                }

                // 에러 메시지에서 대기 시간 추출
                String errorBody = e.getResponseBodyAsString();
                int waitSeconds = extractRetryDelay(errorBody);

                log.warn("⏳ API 할당량 초과, {}초 후 재시도 ({}/{})",
                        waitSeconds, retryCount, maxRetries);

                try {
                    Thread.sleep(waitSeconds * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("❌ 재시도 대기 중 인터럽트");
                    return null;
                }

            } catch (Exception e) {
                log.error("❌ Gemini API 호출 실패: {}", e.getMessage());
                return null; // 실패 시 null 반환
            }
        }

        log.error("❌ 최대 재시도 횟수 초과");
        return null;
    }

    /**
     * 에러 메시지에서 재시도 대기 시간 추출
     */
    private int extractRetryDelay(String errorMessage) {
        try {
            if (errorMessage != null && errorMessage.contains("Please retry in")) {
                String[] parts = errorMessage.split("Please retry in ");
                if (parts.length > 1) {
                    String delayStr = parts[1].split("s")[0].trim();
                    double delaySeconds = Double.parseDouble(delayStr);
                    // 안전하게 5초 추가
                    return (int) Math.ceil(delaySeconds) + 5;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 재시도 대기 시간 파싱 실패", e);
        }
        return 60; // 파싱 실패 시 기본 60초
    }


    /**
     * Gemini 응답에서 위험도 파싱
     */
    private String parseRiskLevel(String response) {
        if (response == null || response.isEmpty()) {
            log.warn("⚠️ 응답이 비어있음");
            return null;
        }

        String upperResponse = response.toUpperCase().trim();

        // 정확히 한 단어만 응답한 경우 (가장 이상적)
        if (upperResponse.equals("CRITICAL")) {
            return "CRITICAL";
        } else if (upperResponse.equals("RISK")) {
            return "RISK";
        } else if (upperResponse.equals("CAUTION")) {
            return "CAUTION";
        } else if (upperResponse.equals("NORMAL")) {
            return "NORMAL";
        }

        // 문장 속에 포함된 경우
        if (upperResponse.contains("CRITICAL")) {
            log.warn("⚠️ 응답에 부가 텍스트 포함: {}", response);
            return "CRITICAL";
        } else if (upperResponse.contains("RISK")) {
            log.warn("⚠️ 응답에 부가 텍스트 포함: {}", response);
            return "RISK";
        } else if (upperResponse.contains("CAUTION")) {
            log.warn("⚠️ 응답에 부가 텍스트 포함: {}", response);
            return "CAUTION";
        } else if (upperResponse.contains("NORMAL")) {
            log.warn("⚠️ 응답에 부가 텍스트 포함: {}", response);
            return "NORMAL";
        }

        log.error("❌ 유효하지 않은 위험도 응답: {}", response);
        return null;
    }

    /**
     * 학생의 학습 데이터를 기반으로 맞춤형 학습 조언 생성
     */
    public String generatePersonalizedAdvice(
            String studentName,
            String departmentName,
            Integer grade,
            Double gpa,
            Double majorGPA,
            Double attendanceRate,
            String gradeTrend,
            List<String> strongAreas,
            List<String> weakAreas) {

        try {
            String prompt = String.format(
                    "당신은 대학교 학습 지원 전문가입니다. 다음 학생의 데이터를 분석하여 구체적이고 실천 가능한 학습 조언을 제공해주세요.\n\n" +
                            "=== 학생 정보 ===\n" +
                            "이름: %s\n" +
                            "학과: %s\n" +
                            "학년: %d학년\n\n" +
                            "=== 학업 성과 ===\n" +
                            "전체 평점: %.2f/4.5\n" +
                            "전공 평점: %.2f/4.5\n" +
                            "출석률: %.1f%%\n" +
                            "성적 추이: %s\n" +
                            "강점 분야: %s\n" +
                            "약점 분야: %s\n\n" +
                            "=== 분석 요청 ===\n" +
                            "1. 현재 학습 상태를 종합적으로 평가해주세요\n" +
                            "2. 강점을 더 발전시킬 수 있는 방법을 제시해주세요\n" +
                            "3. 약점을 보완하기 위한 구체적인 전략을 제안해주세요\n" +
                            "4. 다음 학기 학습 계획에 대한 조언을 해주세요\n" +
                            "5. 4-5문장으로 간결하고 실천 가능한 조언을 작성해주세요\n" +
                            "6. 격려와 동기부여가 될 수 있도록 긍정적인 톤을 유지해주세요\n",
                    studentName,
                    departmentName,
                    grade,
                    gpa,
                    majorGPA,
                    attendanceRate,
                    gradeTrend,
                    String.join(", ", strongAreas.isEmpty() ? List.of("분석 중") : strongAreas),
                    String.join(", ", weakAreas.isEmpty() ? List.of("없음") : weakAreas)
            );

            return callGeminiApi(prompt);

        } catch (Exception e) {
            System.err.println("맞춤형 조언 생성 실패: " + e.getMessage());
            e.printStackTrace();
            return "학습 데이터 분석 중 오류가 발생했습니다. 지속적인 노력으로 더 나은 성과를 기대합니다.";
        }
    }

    /**
     * 과목 추천 이유를 AI로 생성
     */
    public String generateSubjectRecommendReason(
            String studentName,
            String subjectName,
            String subjectType,
            String professorName,
            Double studentGPA,
            List<String> strongAreas,
            List<String> completedSimilarSubjects) {

        try {
            String prompt = String.format(
                    "학생에게 과목을 추천하는 이유를 2-3문장으로 작성해주세요.\n\n" +
                            "학생: %s (평점: %.2f)\n" +
                            "추천 과목: %s (%s)\n" +
                            "담당 교수: %s\n" +
                            "학생의 강점: %s\n" +
                            "이미 수강한 유사 과목: %s\n\n" +
                            "왜 이 학생에게 이 과목이 적합한지 구체적이고 설득력 있게 설명해주세요.",
                    studentName,
                    studentGPA,
                    subjectName,
                    subjectType,
                    professorName,
                    String.join(", ", strongAreas),
                    completedSimilarSubjects.isEmpty() ? "없음" : String.join(", ", completedSimilarSubjects)
            );

            return callGeminiApi(prompt);

        } catch (Exception e) {
            System.err.println("추천 이유 생성 실패: " + e.getMessage());
            return "학생의 학습 이력과 적성을 고려한 추천 과목입니다.";
        }
    }

    /**
     * 학습 전략 제안
     */
    public List<String> generateLearningStrategies(
            Double currentGPA,
            Double targetGPA,
            String gradeTrend,
            Double attendanceRate,
            List<String> weakAreas) {

        try {
            String prompt = String.format(
                    "학생의 학습 전략을 수립해주세요.\n\n" +
                            "현재 평점: %.2f\n" +
                            "목표 평점: %.2f\n" +
                            "성적 추이: %s\n" +
                            "출석률: %.1f%%\n" +
                            "보완 필요 분야: %s\n\n" +
                            "구체적이고 실천 가능한 학습 전략 5가지를 제시해주세요.\n" +
                            "각 전략은 한 문장으로 작성하고, 번호 없이 각 줄마다 하나씩 작성해주세요.",
                    currentGPA,
                    targetGPA,
                    gradeTrend,
                    attendanceRate,
                    String.join(", ", weakAreas.isEmpty() ? List.of("없음") : weakAreas)
            );

            String response = callGeminiApi(prompt);

            // 응답을 줄 단위로 분리
            return Arrays.stream(response.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !s.matches("^\\d+\\..*")) // 번호 제거
                    .limit(5)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("학습 전략 생성 실패: " + e.getMessage());
            return List.of(
                    "꾸준한 출석과 수업 집중",
                    "과제 계획적 수행",
                    "복습 습관 형성",
                    "스터디 그룹 활용",
                    "교수님 면담 정기적 진행"
            );
        }
    }

    /**
     * AI 기반 중도이탈 위험 종합 예측 - 안정성 강화
     */
    public String predictOverallDropoutRisk(AIAnalysisResult result, StuSubDetail detail) {
        try {
            // 데이터 준비
            int absent = detail != null && detail.getAbsent() != null ? detail.getAbsent() : 0;
            int lateness = detail != null && detail.getLateness() != null ? detail.getLateness() : 0;
            int homework = detail != null && detail.getHomework() != null ? detail.getHomework() : 0;
            int midExam = detail != null && detail.getMidExam() != null ? detail.getMidExam() : 0;
            int finalExam = detail != null && detail.getFinalExam() != null ? detail.getFinalExam() : 0;

            String tuitionStatus = result.getTuitionStatus() != null ? result.getTuitionStatus() : "NORMAL";
            String counselingStatus = result.getCounselingStatus() != null ? result.getCounselingStatus() : "NORMAL";

            // ⭐ 개선된 프롬프트: 더 명확한 지시사항
            String prompt = String.format(
                    "당신은 대학생 중도이탈 위험 예측 전문 AI입니다. 다음 학생 데이터를 종합 분석하여 중도이탈 위험도를 정확히 판단해주세요.\n\n" +

                            "=== 학생 학업 데이터 ===\n" +
                            "📊 출석 상황:\n" +
                            "   - 결석: %d회\n" +
                            "   - 지각: %d회\n" +
                            "   - 환산 결석: %.1f회 (지각 3회 = 결석 1회)\n" +
                            "   - 참고: 환산 결석 3회 이상이면 F학점 자동 부여\n\n" +

                            "📝 과제 및 성적:\n" +
                            "   - 과제 점수: %d점 (100점 만점)\n" +
                            "   - 중간고사: %d점 (100점 만점)\n" +
                            "   - 기말고사: %d점 (100점 만점)\n" +
                            "   - 시험 평균: %.1f점\n\n" +

                            "💰 등록금 상태: %s\n" +
                            "   (NORMAL=납부완료, CAUTION=미납, CRITICAL=장기미납)\n\n" +

                            "🗣️ 상담 분석 결과: %s\n" +
                            "   (이미 AI가 상담 내용을 분석한 위험도)\n" +
                            "   (NORMAL=문제없음, CAUTION=주의, RISK=위험, CRITICAL=매우위험)\n\n" +

                            "=== 위험도 판단 기준 (매우 중요!) ===\n\n" +

                            "**CRITICAL** (매우 위험 - 즉각 개입 필요):\n" +
                            "• F학점 확정 가능성이 매우 높음:\n" +
                            "  - 환산 결석 3회 이상 (자동 F학점)\n" +
                            "  - 시험 평균 30점 미만\n" +
                            "  - 중간/기말 둘 다 40점 미만\n" +
                            "• 2개 이상 영역에서 심각한 문제 동시 발생\n" +
                            "• 상담 분석 결과가 CRITICAL\n" +
                            "• 등록금 미납 + 학업 부진 복합\n" +
                            "• 학업 포기 징후가 명확함\n\n" +

                            "**RISK** (위험 - 집중 관리 필요):\n" +
                            "• F학점 가능성이 있음:\n" +
                            "  - 환산 결석 2~2.9회\n" +
                            "  - 시험 평균 30~50점\n" +
                            "  - 과제 40점 미만\n" +
                            "• 1개 영역이 CRITICAL이지만 다른 영역은 괜찮음\n" +
                            "• 2개 이상 영역에서 위험 신호\n" +
                            "• 상담 분석 결과가 RISK\n" +
                            "• 학업 동기 저하가 뚜렷함\n\n" +

                            "**CAUTION** (주의 - 모니터링 필요):\n" +
                            "• 한계선에 있음:\n" +
                            "  - 환산 결석 1~1.9회\n" +
                            "  - 시험 평균 50~65점\n" +
                            "  - 과제 50~70점\n" +
                            "• 1개 영역에서만 위험 신호\n" +
                            "• 일시적 학업 부진\n" +
                            "• 개선 가능성이 있지만 지켜봐야 함\n\n" +

                            "**NORMAL** (정상):\n" +
                            "• 대부분 영역에서 양호\n" +
                            "• 출석률 90%% 이상 (환산 결석 1회 미만)\n" +
                            "• 시험 평균 65점 이상\n" +
                            "• 과제 70점 이상\n" +
                            "• 일부 부족함이 있어도 전체적으로 관리 가능한 수준\n\n" +

                            "=== 중요 판단 원칙 ===\n" +
                            "1. **복합적 상황 우선**: 여러 문제가 동시 발생하면 위험도 상승\n" +
                            "2. **F학점 위험 중시**: 출석 미달(환산 3회)이나 극심한 성적 저하는 CRITICAL\n" +
                            "3. **상담 결과 반영**: 상담에서 심각한 문제가 포착되면 가중\n" +
                            "4. **맥락 고려**: 단일 지표만으로 판단하지 말고 전체 패턴 분석\n" +
                            "5. **보수적 판단**: 애매하면 한 단계 높은 위험도로 판단 (조기 개입이 낫다)\n\n" +

                            "=== 출력 형식 (절대 준수!) ===\n" +
                            "위 데이터를 종합 분석하여 **정확히 한 단어만** 응답하세요:\n" +
                            "CRITICAL, RISK, CAUTION, NORMAL\n\n" +

                            "다른 설명이나 부가 텍스트 없이 위험도 단어 하나만 출력하세요.\n" +
                            "예시: RISK",
                    absent,
                    lateness,
                    absent + (lateness / 3.0),
                    homework,
                    midExam,
                    finalExam,
                    (midExam + finalExam) / 2.0,
                    tuitionStatus,
                    counselingStatus
            );

            log.info("🤖 Gemini API 호출 시작 (종합 위험도 예측)");
            String geminiResponse = callGeminiApi(prompt);

            if (geminiResponse == null || geminiResponse.trim().isEmpty()) {
                log.warn("⚠️ Gemini 응답이 비어있음");
                return null;
            }

            String riskLevel = parseRiskLevel(geminiResponse);

            log.info("✅ AI 예측 완료: 입력=\"{}...\", 응답=\"{}\", 파싱=\"{}\"",
                    geminiResponse.substring(0, Math.min(50, geminiResponse.length())),
                    geminiResponse.trim(),
                    riskLevel);

            return riskLevel;

        } catch (Exception e) {
            log.error("❌ AI 예측 실패: {}", e.getMessage(), e);
            return null; // null 반환 시 폴백 로직 사용
        }
    }

}