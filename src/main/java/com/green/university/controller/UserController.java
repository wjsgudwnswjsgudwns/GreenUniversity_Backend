package com.green.university.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;
import java.util.HashMap;

import com.green.university.dto.CreateProfessorDto;
import com.green.university.dto.CreateStaffDto;
import com.green.university.dto.CreateStudentDto;
import com.green.university.dto.ProfessorListForm;
import com.green.university.dto.StudentListForm;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Student;
import com.green.university.service.ProfessorService;
import com.green.university.service.StudentService;
import com.green.university.service.UserService;

/**
 * 유저 페이지
 * 
 * @author 김지현
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

	@Autowired
	private UserService userService;
	@Autowired
	private StudentService studentService;
	@Autowired
	private ProfessorService professorService;

	/**
	 * @return staff 입력 페이지
	 */
    @GetMapping("/staff")
    public ResponseEntity<Map<String, String>> createStaff() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "직원 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

	/**
	 * staff 입력 post 처리
	 * 
	 * @param createStaffDto
	 * @return "redirect:/user/staff"
	 */
    @PostMapping("/staff")
    public ResponseEntity<?> createStaffProc(@Valid @RequestBody CreateStaffDto createStaffDto) {
        userService.createStaffToStaffAndUser(createStaffDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "직원이 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

	/**
	 * @return professor 입력 페이지
	 */
    @GetMapping("/professor")
    public ResponseEntity<Map<String, String>> createProfessor() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "교수 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

	/**
	 * staff 입력 post 처리
	 * 
	 * @param createProfessorDto
	 * @return "redirect:/user/professor"
	 */
    @PostMapping("/professor")
    public ResponseEntity<?> createProfessorProc(@Valid @RequestBody CreateProfessorDto createProfessorDto) {
        userService.createProfessorToProfessorAndUser(createProfessorDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "교수가 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

	/**
	 * @return student 입력 페이지
	 */
    @GetMapping("/student")
    public ResponseEntity<Map<String, String>> createStudent() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "학생 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

	/**
	 * student 입력 post 처리
	 * 
	 * @param createStudentDto
	 * @return "redirect:/user/student"
	 */
    @PostMapping("/student")
    public ResponseEntity<?> createStudentProc(@Valid @RequestBody CreateStudentDto createStudentDto) {
        userService.createStudentToStudentAndUser(createStudentDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "학생이 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

	/**
	 * 교수 조회
	 * 
	 * @param model
	 * @return 교수 조회 페이지
	 */
    @GetMapping("/professorList")
    public ResponseEntity<?> showProfessorList(@RequestParam(required = false) Integer professorId,
            @RequestParam(required = false) Integer deptId) {
        ProfessorListForm professorListForm = new ProfessorListForm();
        professorListForm.setPage(0);
        if (professorId != null) {
            professorListForm.setProfessorId(professorId);
        } else if (deptId != null) {
            professorListForm.setDeptId(deptId);
        }
        Integer amount = professorService.readProfessorAmount(professorListForm);
        if (professorId != null) {
            amount = 1;
        }
        List<Professor> list = professorService.readProfessorList(professorListForm);
        Map<String, Object> body = new HashMap<>();
        body.put("listCount", Math.ceil(amount / 20.0));
        body.put("professorList", list);
        body.put("deptId", deptId);
        body.put("page", 1);
        return ResponseEntity.ok(body);
    }

	/**
	 * 교수 조회
	 * 
	 * @param model
	 * @return 교수 조회 페이지
	 */
    @GetMapping("/professorList/{page}")
    public ResponseEntity<?> showProfessorListByPage(@PathVariable Integer page,
            @RequestParam(required = false) Integer deptId) {
        ProfessorListForm professorListForm = new ProfessorListForm();
        if (deptId != null) {
            professorListForm.setDeptId(deptId);
        }
        professorListForm.setPage((page - 1) * 20);
        Integer amount = professorService.readProfessorAmount(professorListForm);
        List<Professor> list = professorService.readProfessorList(professorListForm);
        Map<String, Object> body = new HashMap<>();
        body.put("listCount", Math.ceil(amount / 20.0));
        body.put("professorList", list);
        body.put("page", page);
        return ResponseEntity.ok(body);
    }

	/**
	 * 학생 조회
	 * 
	 * @param model
	 * @return 학생 조회 페이지
	 */
    @GetMapping("/studentList")
    public ResponseEntity<?> showStudentList(@RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer deptId) {
        StudentListForm studentListForm = new StudentListForm();
        studentListForm.setPage(0);
        if (studentId != null) {
            studentListForm.setStudentId(studentId);
        } else if (deptId != null) {
            studentListForm.setDeptId(deptId);
        }
        Integer amount = studentService.readStudentAmount(studentListForm);
        if (studentId != null) {
            amount = 1;
        }
        List<Student> list = studentService.readStudentList(studentListForm);
        Map<String, Object> body = new HashMap<>();
        body.put("listCount", Math.ceil(amount / 20.0));
        body.put("studentList", list);
        body.put("deptId", deptId);
        body.put("page", 1);
        return ResponseEntity.ok(body);
    }

	/**
	 * 학생 조회
	 * 
	 * @param model
	 * @return 학생 조회 페이지
	 */
    @GetMapping("/studentList/{page}")
    public ResponseEntity<?> showStudentListByPage(@PathVariable Integer page,
            @RequestParam(required = false) Integer deptId) {
        StudentListForm studentListForm = new StudentListForm();
        if (deptId != null) {
            studentListForm.setDeptId(deptId);
        }
        studentListForm.setPage((page - 1) * 20);
        Integer amount = studentService.readStudentAmount(studentListForm);
        List<Student> list = studentService.readStudentList(studentListForm);
        Map<String, Object> body = new HashMap<>();
        body.put("listCount", Math.ceil(amount / 20.0));
        body.put("studentList", list);
        body.put("page", page);
        return ResponseEntity.ok(body);
    }

	/**
	 * 학생의 학년, 학기 업데이트
	 * 
	 * @return 학생 리스트 조회 페이지
	 */
    @GetMapping("/student/update")
    public ResponseEntity<Map<String, String>> updateStudentGradeAndSemester() {
        studentService.updateStudentGradeAndSemester();
        Map<String, String> body = new HashMap<>();
        body.put("message", "학생 학년과 학기가 업데이트되었습니다.");
        return ResponseEntity.ok(body);
    }

}
