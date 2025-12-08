package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;

import com.green.university.dto.response.GradeDto;
import com.green.university.dto.response.MyGradeDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.service.GradeService;
import com.green.university.utils.Define;

// 금학기,학기별 성적, 누계성적 조회

// 성적 관련 REST API 컨트롤러입니다. 금학기 성적 조회, 학기별 성적 조회, 총 누계 성적
// 조회 기능을 JSON 응답으로 제공합니다.
@RestController
@RequestMapping("/api/grade")
public class GradeController {

	@Autowired
	private HttpSession session;

	@Autowired
	private GradeService gradeService;

    @GetMapping("/thisSemester")
    public ResponseEntity<Map<String, Object>> thisSemester() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> yearList = gradeService.readGradeYearByStudentId(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);
        if (!yearList.isEmpty()) {
            List<GradeDto> thisSemester = gradeService.readThisSemesterByStudentId(principal.getId());
            MyGradeDto mygrade = gradeService.readMyGradeByStudentId(principal.getId());
            body.put("gradeList", thisSemester);
            body.put("mygrade", mygrade);
        }
        return ResponseEntity.ok(body);
    }

    @GetMapping("/semester")
    public ResponseEntity<Map<String, Object>> semester() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> yearList = gradeService.readGradeYearByStudentId(principal.getId());
        List<GradeDto> gradeAllList = gradeService.readAllGradeByStudentId(principal.getId());
        List<Integer> semesterList = gradeService.readGradeSemesterByStudentId(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);
        body.put("gradeList", gradeAllList);
        body.put("semesterList", semesterList);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> readGradeProc(@RequestParam String type,
                                                             @RequestParam int subYear, @RequestParam int sesmeter) {
        System.out.println("=== GradeController.thisSemester 진입 ===");
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> yearList = gradeService.readGradeYearByStudentId(principal.getId());
        List<Integer> semesterList = gradeService.readGradeSemesterByStudentId(principal.getId());
        Map<String, Object> body = new HashMap<>();
        if ("전체".equals(type)) {
            List<GradeDto> gradeAllList = gradeService.readGradeByStudentId(principal.getId(), subYear, sesmeter);
            body.put("gradeList", gradeAllList);
        } else {
            List<GradeDto> gradeList = gradeService.readGradeByType(principal.getId(), subYear, sesmeter, type);
            body.put("gradeList", gradeList);
        }
        body.put("yearList", yearList);
        body.put("semesterList", semesterList);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> totalGrade() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> yearList = gradeService.readGradeYearByStudentId(principal.getId());
        List<MyGradeDto> mygradeList = gradeService.readgradeinquiryList(principal.getId());
        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);
        body.put("mygradeList", mygradeList);
        return ResponseEntity.ok(body);
    }

}
