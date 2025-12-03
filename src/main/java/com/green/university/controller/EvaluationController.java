package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

import com.green.university.dto.EvaluationDto;
import com.green.university.dto.MyEvaluationDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.QuestionDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.EvaluationService;
import com.green.university.service.QuestionService;
import com.green.university.utils.Define;

/**
 * 강의 평가와 관련된 REST API를 제공하는 컨트롤러입니다. 기존 JSP 기반 구현을
 * JSON 응답 기반 구조로 전환하여 리액트와 같은 프론트엔드에서 데이터를 쉽게 소비할 수
 * 있도록 했습니다.
 */
@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

	@Autowired
	private HttpSession session;
	@Autowired
	private EvaluationService evaluationService;
	@Autowired
	private QuestionService questionService;

    /**
     * 강의 평가 질문을 조회합니다. 과목 ID를 전달하면 해당 과목에 대한 질문과 식별자를
     * 반환합니다.
     *
     * @param subjectId 과목 식별자
     * @return 질문 목록과 과목 ID를 포함하는 JSON 응답
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> evaluation(@RequestParam(required = false) Integer subjectId) {
        QuestionDto dto = questionService.readQuestion();
        Map<String, Object> body = new HashMap<>();
        body.put("subjectId", subjectId);
        body.put("dto", dto);
        return ResponseEntity.ok(body);
    }

	/*
	 * 강의평가 post
	 */
    @PostMapping("/write/{subjectId}")
    public ResponseEntity<?> EvaluationProc(@PathVariable Integer subjectId, @RequestBody EvaluationDto evaluationFormDto) {
		PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);

		evaluationFormDto.setStudentId(principal.getId());
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
        } else {
            evaluationService.createEvanluation(evaluationFormDto);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("message", "강의 평가가 등록되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	// 강의 평가 처음화면 (교수)
    @GetMapping("/read")
    public ResponseEntity<Map<String, Object>> readEvaluation() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<String> subjectName = evaluationService.readSubjectName(principal.getId());
        List<MyEvaluationDto> eval = evaluationService.readEvaluationByProfessorId(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("subjectName", subjectName);
        body.put("eval", eval);
        return ResponseEntity.ok(body);
    }

	// 과목별 강의 평가 조회 (교수)
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> readEvaluation(@RequestParam String subjectId) {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<String> subjectName = evaluationService.readSubjectName(principal.getId());
        List<MyEvaluationDto> eval = evaluationService.readEvaluationByProfessorIdAndName(principal.getId(), subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("subjectName", subjectName);
        body.put("eval", eval);
        return ResponseEntity.ok(body);
    }

}
