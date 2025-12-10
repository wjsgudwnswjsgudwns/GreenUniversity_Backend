package com.green.university.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.service.ChatbotService;

/**
 * 챗봇 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    /**
     * Authentication에서 학생 ID 추출
     */
    private Integer getStudentId(Authentication authentication) {
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        return principal.getId();
    }

    /**
     * 챗봇 메시지 처리
     * 
     * @param message 사용자가 입력한 메시지
     * @param authentication 인증 정보
     * @return 챗봇 응답
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> processMessage(
            @RequestParam String message,
            Authentication authentication) {
        
        Integer studentId = getStudentId(authentication);
        String response = chatbotService.processMessage(studentId, message);

        Map<String, Object> body = new HashMap<>();
        body.put("response", response);
        body.put("studentId", studentId);

        return ResponseEntity.ok(body);
    }

    /**
     * 챗봇 초기 인사말
     */
    @GetMapping("/greeting")
    public ResponseEntity<Map<String, Object>> getGreeting(Authentication authentication) {
        String greeting = "안녕하세요! 그린대학교 챗봇입니다. 😊\n\n" +
                         "등록 여부, 수강 신청, 학점, 졸업 요건 등에 대해 물어보실 수 있습니다.\n" +
                         "무엇을 도와드릴까요?";

        Map<String, Object> body = new HashMap<>();
        body.put("response", greeting);

        return ResponseEntity.ok(body);
    }
}


