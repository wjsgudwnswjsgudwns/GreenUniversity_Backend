package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import com.green.university.repository.model.StuSub;
import com.green.university.repository.model.SyllaBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.green.university.dto.SyllaBusFormDto;
import com.green.university.dto.UpdateStudentGradeDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.ReadSyllabusDto;
import com.green.university.dto.response.StudentInfoForProfessorDto;
import com.green.university.dto.response.SubjectForProfessorDto;
import com.green.university.dto.response.SubjectPeriodForProfessorDto;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Subject;
import com.green.university.service.ProfessorService;
import com.green.university.service.StuSubService;
import com.green.university.service.SubjectService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;

/**
 * 교수 행정 페이지 (자기과목 조회, 학생 성적 기입)
 * 
 * @author 김지현
 */
/**
 * 교수 행정 페이지 관련 REST 컨트롤러입니다. 과목 조회, 성적 입력, 강의계획서
 * 관리 등을 JSON 형식으로 제공합니다.
 */
@RestController
@RequestMapping("/api/professor")
public class ProfessorController {

	@Autowired
	private ProfessorService professorService;
	@Autowired
	private HttpSession session;
	@Autowired
	private UserService userService;
	@Autowired
	private StuSubService stuSubService;
	@Autowired
	private SubjectService subjectService;
	
	/**
	 * 본인의 강의가 있는 년도 학기 조회하는 기능 조회한 년도 학기의 강의 리스트 출력(처음값은 현재학기)
	 * 
	 * @param model
	 * @return 본인 강좌 조회 페이지
	 */
    @GetMapping("/subject")
    public ResponseEntity<Map<String, Object>> subjectList() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> semesterList = professorService.selectSemester(principal.getId());
        SubjectPeriodForProfessorDto subjectPeriodForProfessorDto = new SubjectPeriodForProfessorDto();
        subjectPeriodForProfessorDto.setSubYear(Define.CURRENT_YEAR);
        subjectPeriodForProfessorDto.setSemester(Define.CURRENT_SEMESTER);
        subjectPeriodForProfessorDto.setId(principal.getId());
        List<Subject> subjectList = professorService
                .selectSubjectBySemester(subjectPeriodForProfessorDto);
        Map<String, Object> body = new HashMap<>();
        body.put("semesterList", semesterList);
        body.put("subjectList", subjectList);
        return ResponseEntity.ok(body);
    }

	/**
	 * 조회한 년도 학기의 강의 리스트 출력
	 * 
	 * @param model
	 * @param period: 조회할 년도 학기
	 * @return 조회 신청한 학기의 본인 강좌 조회 페이지
	 */
    @PostMapping("/subject")
    public ResponseEntity<Map<String, Object>> subjectListProc(@RequestParam String period) {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Integer> semesterList = professorService.selectSemester(principal.getId());
        String[] strs = period.split("year");
        SubjectPeriodForProfessorDto subjectPeriodForProfessorDto = new SubjectPeriodForProfessorDto();
        subjectPeriodForProfessorDto.setSubYear(Integer.parseInt(strs[0]));
        subjectPeriodForProfessorDto.setSemester(Integer.parseInt(strs[1]));
        subjectPeriodForProfessorDto.setId(principal.getId());
        List<Subject> subjectList = professorService.selectSubjectBySemester(subjectPeriodForProfessorDto);
        Map<String, Object> body = new HashMap<>();
        body.put("semesterList", semesterList);
        body.put("subjectList", subjectList);
        return ResponseEntity.ok(body);
    }

	/**
	 * 
	 * @param model
	 * @return 해당 과목을 듣는 학생 리스트
	 */
    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<Map<String, Object>> subjectStudentList(@PathVariable Integer subjectId) {
        List<StuSub> studentList = professorService.selectBySubjectId(subjectId);
        Subject subject = professorService.selectSubjectById(subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("studentList", studentList);
        body.put("subject", subject);
        return ResponseEntity.ok(body);
    }

	/**
	 * 
	 * @param model
	 * @param subjectId
	 * @param studentId
	 * @return 출결 및 성적 기입 페이지
	 */
    @GetMapping("/subject/{subjectId}/{studentId}")
    public ResponseEntity<Map<String, Object>> updateStudentDetail(@PathVariable Integer subjectId,
            @PathVariable Integer studentId) {
        Student student = userService.readStudent(studentId);
        Map<String, Object> body = new HashMap<>();
        body.put("student", student);
        body.put("subjectId", subjectId);
        return ResponseEntity.ok(body);
    }

    @PutMapping("/subject/{subjectId}/{studentId}")
    public ResponseEntity<Map<String, String>> updateStudentDetailProc(@PathVariable Integer subjectId,
            @PathVariable Integer studentId, @RequestBody UpdateStudentGradeDto updateStudentGradeDto) {
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
	 * 
	 * @param model
	 * @return 강의계획서 업데이트 창
	 */
    @GetMapping("/syllabus/update/{subjectId}")
    public ResponseEntity<Map<String, Object>> createSyllabus(@PathVariable Integer subjectId) {
        SyllaBus readSyllabusDto = professorService.readSyllabus(subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("syllabus", readSyllabusDto);
        return ResponseEntity.ok(body);
    }

	/**
	 * 
	 * @param syllaBusFormDto
	 * @return 강의계획서창
	 */
    @PutMapping("/syllabus/update/{subjectId}")
    public ResponseEntity<Map<String, String>> createSyllabusProc(@RequestBody SyllaBusFormDto syllaBusFormDto) {
        professorService.updateSyllabus(syllaBusFormDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "강의 계획서가 수정되었습니다.");
        return ResponseEntity.ok(body);
    }

}
