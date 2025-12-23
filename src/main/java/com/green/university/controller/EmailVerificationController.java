package com.green.university.controller;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailService emailService;

    /**
     * 현재 인증된 사용자 정보 조회
     */
    private PrincipalDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new CustomRestfullException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
        }

        Object principalObj = authentication.getPrincipal();

        if (principalObj instanceof PrincipalDto) {
            return (PrincipalDto) principalObj;
        }

        throw new CustomRestfullException("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
    }

    /**
     * 이메일 인증 코드 발송
     * POST /api/email/send
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendVerificationCode(@RequestBody Map<String, String> request) {
        PrincipalDto principal = getCurrentUser();
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            throw new CustomRestfullException("이메일을 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        // 이메일 유효성 검사
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new CustomRestfullException("올바른 이메일 형식이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        emailService.sendVerificationEmail(principal.getId(), email);

        Map<String, String> body = new HashMap<>();
        body.put("message", "인증 코드가 이메일로 발송되었습니다. (유효시간: 5분)");
        return ResponseEntity.ok(body);
    }

    /**
     * 이메일 인증 코드 확인
     * POST /api/email/verify
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody Map<String, String> request) {
        PrincipalDto principal = getCurrentUser();
        String email = request.get("email");
        String code = request.get("code");

        if (email == null || email.trim().isEmpty()) {
            throw new CustomRestfullException("이메일을 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        if (code == null || code.trim().isEmpty()) {
            throw new CustomRestfullException("인증 코드를 입력해주세요.", HttpStatus.BAD_REQUEST);
        }

        boolean verified = emailService.verifyEmailCode(principal.getId(), email, code);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "이메일 인증이 완료되었습니다.");
        body.put("verified", verified);
        return ResponseEntity.ok(body);
    }

    /**
     * 이메일 인증 상태 확인
     * GET /api/email/status?email=test@example.com
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkVerificationStatus(@RequestParam String email) {
        PrincipalDto principal = getCurrentUser();

        boolean verified = emailService.isEmailVerified(principal.getId(), email);

        Map<String, Object> body = new HashMap<>();
        body.put("verified", verified);
        body.put("email", email);
        return ResponseEntity.ok(body);
    }
}