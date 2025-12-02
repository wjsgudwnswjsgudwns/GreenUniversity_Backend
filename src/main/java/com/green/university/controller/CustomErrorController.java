package com.green.university.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

/**
 * REST 기반 오류 처리 컨트롤러입니다. 기존 JSP 페이지를 반환하는 대신 JSON 형식의 오류
 * 메시지를 반환하여 프론트엔드 애플리케이션과의 호환성을 높였습니다.
 */
@RestController
@RequestMapping("/api/error")
public class CustomErrorController {

    /**
     * 기본 오류 엔드포인트. 단순히 오류 메시지를 포함한 JSON 응답을 반환합니다.
     *
     * @return 오류 메시지를 담은 응답 객체
     */
    @GetMapping("")
    public ResponseEntity<Map<String, String>> handleError() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "요청 처리 중 오류가 발생했습니다.");
        return ResponseEntity.status(404).body(body);
    }
}
