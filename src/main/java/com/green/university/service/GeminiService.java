package com.green.university.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 상담 내용을 Gemini AI로 분석하여 위험도 반환
     * @param counselingContent 상담 내용
     * @return "CRITICAL", "RISK", "CAUTION", "NORMAL"
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
                            "=== 중요 ===\n" +
                            "- \"로또 당첨으로 등록금 문제 해결\" → NORMAL (문제가 해결됨)\n" +
                            "- \"등록금 낼 돈이 없다\" → CRITICAL (현재 심각한 문제)\n" +
                            "- \"성적이 올랐다\" → NORMAL (긍정적 변화)\n" +
                            "- \"계속 F학점만 받는다\" → RISK (지속적 문제)\n\n" +
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
            return "NORMAL"; // 실패 시 기본값
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
}