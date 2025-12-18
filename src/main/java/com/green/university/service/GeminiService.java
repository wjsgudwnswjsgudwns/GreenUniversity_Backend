package com.green.university.service;

import com.green.university.repository.model.AIAnalysisResult;
import com.green.university.repository.model.StuSubDetail;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;
import java.util.stream.Collectors;

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
     * Gemini API 호출
     */
    private String callGeminiApi(String prompt) {
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

            return "NORMAL";

        } catch (Exception e) {
            System.err.println("Gemini API 호출 실패: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Gemini API 호출 실패", e);
        }
    }

    /**
     * Gemini 응답에서 위험도 파싱
     */
    private String parseRiskLevel(String response) {
        if (response == null || response.isEmpty()) {
            return "NORMAL";
        }

        String upperResponse = response.toUpperCase().trim();

        if (upperResponse.contains("CRITICAL")) {
            return "CRITICAL";
        } else if (upperResponse.contains("RISK")) {
            return "RISK";
        } else if (upperResponse.contains("CAUTION")) {
            return "CAUTION";
        } else {
            return "NORMAL";
        }
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
}