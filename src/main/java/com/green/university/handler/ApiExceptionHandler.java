package com.green.university.handler;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.handler.exception.UnAuthorizedException;

@RestControllerAdvice
public class ApiExceptionHandler {

    // JSON Body 없음 / 잘못된 JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException e,
                                                          HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "요청 형식이 올바르지 않습니다.(JSON Body 확인)");
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // @Valid 검증 실패 (LoginDto 포함)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException e,
                                              HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "요청 데이터가 올바르지 않습니다.");
        body.put("path", request.getRequestURI());

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        body.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 우리 프로젝트에서 이미 쓰던 커스텀 예외 (로그인 실패, 권한 문제 등)
    @ExceptionHandler(CustomRestfullException.class)
    public ResponseEntity<?> handleCustom(CustomRestfullException e,
                                          HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(e.getStatus()).body(body);
    }

    @ExceptionHandler(UnAuthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnAuthorizedException e,
                                                HttpServletRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", e.getMessage());
        body.put("path", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

}
