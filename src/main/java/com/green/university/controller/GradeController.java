package com.green.university.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.green.university.dto.response.GradeDto;
import com.green.university.dto.response.MyGradeDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.service.GradeService;

/**
 * 성적 관련 REST API 컨트롤러 (JWT 기반)
 * 금학기 성적 조회, 학기별 성적 조회, 총 누계 성적 조회 기능 제공
 */
@RestController
@RequestMapping("/api/grade")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    /**
     * Authentication에서 학생 ID 추출
     */
    private Integer getStudentId(Authentication authentication) {
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        return principal.getId();
    }

    /**
     * 금학기 성적 조회
     */
    @GetMapping("/thisSemester")
    public ResponseEntity<Map<String, Object>> thisSemester(Authentication authentication) {
        Integer studentId = getStudentId(authentication);

        List<Integer> yearList = gradeService.readGradeYearByStudentId(studentId);
        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);

        if (!yearList.isEmpty()) {
            List<GradeDto> thisSemester = gradeService.readThisSemesterByStudentId(studentId);
            MyGradeDto mygrade = gradeService.readMyGradeByStudentId(studentId);
            body.put("gradeList", thisSemester);
            body.put("mygrade", mygrade);
        }

        return ResponseEntity.ok(body);
    }

    /**
     * 학기별 성적 조회 (초기 데이터)
     */
    @GetMapping("/semester")
    public ResponseEntity<Map<String, Object>> semester(Authentication authentication) {
        Integer studentId = getStudentId(authentication);

        List<Integer> yearList = gradeService.readGradeYearByStudentId(studentId);
        List<GradeDto> gradeAllList = gradeService.readAllGradeByStudentId(studentId);
        List<Integer> semesterList = gradeService.readGradeSemesterByStudentId(studentId);

        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);
        body.put("gradeList", gradeAllList);
        body.put("semesterList", semesterList);

        return ResponseEntity.ok(body);
    }

    /**
     * 학기별 성적 조회 (필터링)
     */
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> readGradeProc(
            @RequestParam String type,
            @RequestParam int subYear,
            @RequestParam int semester,
            Authentication authentication) {

        Integer studentId = getStudentId(authentication);

        List<Integer> yearList = gradeService.readGradeYearByStudentId(studentId);
        List<Integer> semesterList = gradeService.readGradeSemesterByStudentId(studentId);

        Map<String, Object> body = new HashMap<>();

        if ("전체".equals(type)) {
            List<GradeDto> gradeAllList = gradeService.readGradeByStudentId(studentId, subYear, semester);
            body.put("gradeList", gradeAllList);
        } else {
            List<GradeDto> gradeList = gradeService.readGradeByType(studentId, subYear, semester, type);
            body.put("gradeList", gradeList);
        }

        body.put("yearList", yearList);
        body.put("semesterList", semesterList);

        return ResponseEntity.ok(body);
    }

    /**
     * 총 누계 성적 조회
     */
    @GetMapping("/total")
    public ResponseEntity<Map<String, Object>> totalGrade(Authentication authentication) {
        Integer studentId = getStudentId(authentication);

        List<Integer> yearList = gradeService.readGradeYearByStudentId(studentId);
        List<MyGradeDto> mygradeList = gradeService.readgradeinquiryList(studentId);

        Map<String, Object> body = new HashMap<>();
        body.put("yearList", yearList);
        body.put("mygradeList", mygradeList);

        return ResponseEntity.ok(body);
    }
}