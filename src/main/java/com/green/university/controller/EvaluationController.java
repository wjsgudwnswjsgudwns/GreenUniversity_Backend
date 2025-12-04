package com.green.university.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.green.university.dto.EvaluationDto;
import com.green.university.dto.MyEvaluationDto;
import com.green.university.dto.response.QuestionDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.EvaluationService;
import com.green.university.service.QuestionService;

/**
 * 강의 평가와 관련된 REST API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    @Autowired
    private EvaluationService evaluationService;

    @Autowired
    private QuestionService questionService;

    /**
     * 강의 평가 질문을 조회합니다.
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> evaluation(
            @RequestParam(required = false) Integer subjectId) {

        QuestionDto dto = questionService.readQuestion();

        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", subjectId);
        body.put("dto", dto);
        return ResponseEntity.ok(body);
    }

    /**
     * 강의평가 제출
     */
    @PostMapping("/write/{subjectId}")
    public ResponseEntity<Map<String, String>> evaluationProc(
            @PathVariable Integer subjectId,
            @RequestBody EvaluationDto evaluationFormDto,
            Authentication authentication) {

        Integer studentId = Integer.parseInt(authentication.getName());

        evaluationFormDto.setStudentId(studentId);
        evaluationFormDto.setSubjectId(subjectId);

        if (evaluationFormDto.getAnswer1() == null) {
            throw new CustomRestfullException("1번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer2() == null) {
            throw new CustomRestfullException("2번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer3() == null) {
            throw new CustomRestfullException("3번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer4() == null) {
            throw new CustomRestfullException("4번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer5() == null) {
            throw new CustomRestfullException("5번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer6() == null) {
            throw new CustomRestfullException("6번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        } else if (evaluationFormDto.getAnswer7() == null) {
            throw new CustomRestfullException("7번 질문에 답 해주세요", HttpStatus.BAD_REQUEST);
        }

        evaluationService.createEvanluation(evaluationFormDto);

        Map<String, String> body = new HashMap<>();
        body.put("message", "강의 평가가 등록되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * 강의 평가 처음화면 (교수)
     */
    @GetMapping("/read")
    public ResponseEntity<Map<String, Object>> readEvaluation(Authentication authentication) {
        Integer professorId = Integer.parseInt(authentication.getName());

        List<String> subjectName = evaluationService.readSubjectName(professorId);
        List<MyEvaluationDto> eval = evaluationService.readEvaluationByProfessorId(professorId);

        Map<String, Object> body = new HashMap<>();
        body.put("subjectName", subjectName);
        body.put("eval", eval);
        return ResponseEntity.ok(body);
    }

    /**
     * 과목별 강의 평가 조회 (교수)
     */
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> readEvaluationBySubject(
            @RequestParam String subjectId,
            Authentication authentication) {

        Integer professorId = Integer.parseInt(authentication.getName());

        List<String> subjectName = evaluationService.readSubjectName(professorId);
        List<MyEvaluationDto> eval = evaluationService.readEvaluationByProfessorIdAndName(
                professorId,
                subjectId
        );

        Map<String, Object> body = new HashMap<>();
        body.put("subjectName", subjectName);
        body.put("eval", eval);
        return ResponseEntity.ok(body);
    }
}