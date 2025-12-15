package com.green.university.controller;

import java.util.List;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.StuSubResponseDto;
import com.green.university.dto.response.SubjectPeriodForProfessorDto;
import com.green.university.dto.response.SyllabusResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.green.university.dto.SyllaBusFormDto;
import com.green.university.dto.UpdateStudentGradeDto;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.StuSub;
import com.green.university.repository.model.Subject;
import com.green.university.repository.model.SyllaBus;
import com.green.university.service.ProfessorService;
import com.green.university.service.StuSubService;
import com.green.university.service.SubjectService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;

import java.util.HashMap;
import java.util.Map;

/**
 * 교수 행정 페이지 관련 REST 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

    @Autowired
    private ProfessorService professorService;
    @Autowired
    private UserService userService;
    @Autowired
    private StuSubService stuSubService;
    @Autowired
    private SubjectService subjectService;

    /**
     * 본인의 강의가 있는 년도 학기 조회
     */
    @GetMapping("/subject")
    public ResponseEntity<Map<String, Object>> subjectList(Authentication authentication) {
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        Integer professorId = principal.getId();

        List<SubjectPeriodForProfessorDto> semesterList = professorService.selectSemester(professorId);

        // 현재 학기 강의 조회
        List<Subject> subjectList = professorService.selectSubjectBySemester(
                new SubjectPeriodForProfessorDto(
                        professorId,
                        Define.CURRENT_YEAR,
                        Define.CURRENT_SEMESTER
                )
        );

        Map<String, Object> body = new HashMap<>();
        body.put("semesterList", semesterList);
        body.put("subjectList", subjectList);
        return ResponseEntity.ok(body);
    }

    /**
     * 조회한 년도 학기의 강의 리스트 출력
     */
    @PostMapping("/subject")
    public ResponseEntity<Map<String, Object>> subjectListProc(
            @RequestParam String period,
            Authentication authentication) {

        Integer professorId = Integer.parseInt(authentication.getName());
        List<SubjectPeriodForProfessorDto> semesterList = professorService.selectSemester(professorId);

        String[] strs = period.split("year");
        List<Subject> subjectList = professorService.selectSubjectBySemester(
                new SubjectPeriodForProfessorDto(
                        professorId,
                        Integer.parseInt(strs[0]),
                        Integer.parseInt(strs[1])
                )
        );

        Map<String, Object> body = new HashMap<>();
        body.put("semesterList", semesterList);
        body.put("subjectList", subjectList);
        return ResponseEntity.ok(body);
    }

    /**
     * 해당 과목을 듣는 학생 리스트
     */
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<Map<String, Object>> subjectStudentList(@PathVariable Integer subjectId) {
        List<StuSubResponseDto> studentList = professorService.selectBySubjectId(subjectId);
        Subject subject = professorService.selectSubjectById(subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("studentList", studentList);
        body.put("subject", subject);
        return ResponseEntity.ok(body);
    }

    /**
     * ✅ 해당 과목의 실제 수강신청한 학생만 조회 (stu_sub_tb 기준)
     * 예비 수강신청이 아닌 실제 수강신청한 학생만 반환
     */
    @GetMapping("/subject/{subjectId}/enrolled")
    public ResponseEntity<Map<String, Object>> enrolledStudentList(@PathVariable Integer subjectId) {
        List<StuSubResponseDto> studentList = professorService.selectEnrolledStudentsBySubjectId(subjectId);
        Subject subject = professorService.selectSubjectById(subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("studentList", studentList);
        body.put("subject", subject);
        return ResponseEntity.ok(body);
    }

    /**
     * 출결 및 성적 기입 페이지
     */
    @GetMapping("/subject/{subjectId}/{studentId}")
    public ResponseEntity<Map<String, Object>> updateStudentDetail(
            @PathVariable Integer subjectId,
            @PathVariable Integer studentId) {

        Student student = userService.readStudent(studentId);

        Map<String, Object> body = new HashMap<>();
        body.put("student", student);
        body.put("subjectId", subjectId);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/subject/{subjectId}/{studentId}")
    public ResponseEntity<Map<String, String>> updateStudentDetailProc(
            @PathVariable Integer subjectId,
            @PathVariable Integer studentId,
            @RequestBody UpdateStudentGradeDto updateStudentGradeDto) {

        professorService.updateGrade(updateStudentGradeDto);

        if ("F".equals(updateStudentGradeDto.getGrade())) {
            stuSubService.updateCompleteGrade(studentId, subjectId, 0);
        } else {
            Integer subjectGrade = subjectService.readBySubjectId(subjectId).getGrades();
            stuSubService.updateCompleteGrade(studentId, subjectId, subjectGrade);
        }

        Map<String, String> body = new HashMap<>();
        body.put("message", "성적이 수정되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 강의계획서 업데이트 창
     */
    @GetMapping("/syllabus/update/{subjectId}")
    public ResponseEntity<Map<String, Object>> createSyllabus(@PathVariable Integer subjectId) {
        SyllabusResponseDto readSyllabusDto = professorService.readSyllabus(subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("syllabus", readSyllabusDto);
        return ResponseEntity.ok(body);
    }

    /**
     * 강의계획서 수정
     */
    @PutMapping("/syllabus/update/{subjectId}")
    public ResponseEntity<Map<String, String>> createSyllabusProc(
            @RequestBody SyllaBusFormDto syllaBusFormDto) {

        professorService.updateSyllabus(syllaBusFormDto);

        Map<String, String> body = new HashMap<>();
        body.put("message", "강의 계획서가 수정되었습니다.");
        return ResponseEntity.ok(body);
    }
}