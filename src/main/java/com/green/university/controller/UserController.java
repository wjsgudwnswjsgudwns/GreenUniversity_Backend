package com.green.university.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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

    // staff 입력 페이지
    @GetMapping("/staff")
    public ResponseEntity<Map<String, String>> createStaff() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "직원 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

    // staff 입력 post 처리
    @PostMapping("/staff")
    public ResponseEntity<?> createStaffProc(@Valid @RequestBody CreateStaffDto createStaffDto) {
        userService.createStaffToStaffAndUser(createStaffDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "직원이 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // professor 입력 페이지
    @GetMapping("/professor")
    public ResponseEntity<Map<String, String>> createProfessor() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "교수 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

    // professor 입력 post 처리
    @PostMapping("/professor")
    public ResponseEntity<?> createProfessorProc(@Valid @RequestBody CreateProfessorDto createProfessorDto) {
        userService.createProfessorToProfessorAndUser(createProfessorDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "교수가 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // student 입력 페이지
    @GetMapping("/student")
    public ResponseEntity<Map<String, String>> createStudent() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "학생 정보 입력을 위한 페이지입니다.");
        return ResponseEntity.ok(body);
    }

    // student 입력 post 처리
    @PostMapping("/student")
    public ResponseEntity<?> createStudentProc(@Valid @RequestBody CreateStudentDto createStudentDto) {
        userService.createStudentToStudentAndUser(createStudentDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "학생이 생성되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // 교수 조회 (첫 페이지 또는 필터링)
    @GetMapping("/professorList")
    public ResponseEntity<?> showProfessorList(
            @RequestParam(required = false) Integer professorId,
            @RequestParam(required = false) Integer deptId) {

        ProfessorListForm professorListForm = new ProfessorListForm();
        professorListForm.setPage(0); // 첫 페이지는 0
        professorListForm.setProfessorId(professorId);
        professorListForm.setDeptId(deptId);

        Page<Professor> professorPage = professorService.readProfessorList(professorListForm);

        Map<String, Object> body = new HashMap<>();
        body.put("totalPages", professorPage.getTotalPages());
        body.put("totalElements", professorPage.getTotalElements());
        body.put("currentPage", 1); // 사용자에게는 1부터 시작
        body.put("professorList", professorPage.getContent());
        body.put("deptId", deptId);
        body.put("professorId", professorId);

        return ResponseEntity.ok(body);
    }

    // 교수 조회 (페이지별)
    @GetMapping("/professorList/{page}")
    public ResponseEntity<?> showProfessorListByPage(
            @PathVariable Integer page,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) Integer professorId) {

        ProfessorListForm professorListForm = new ProfessorListForm();
        professorListForm.setPage(page - 1); // 0-based index
        professorListForm.setDeptId(deptId);
        professorListForm.setProfessorId(professorId);

        Page<Professor> professorPage = professorService.readProfessorList(professorListForm);

        Map<String, Object> body = new HashMap<>();
        body.put("totalPages", professorPage.getTotalPages());
        body.put("totalElements", professorPage.getTotalElements());
        body.put("currentPage", page);
        body.put("professorList", professorPage.getContent());
        body.put("deptId", deptId);
        body.put("professorId", professorId);

        return ResponseEntity.ok(body);
    }

    // 학생 조회 (첫 페이지 또는 필터링)
    @GetMapping("/studentList")
    public ResponseEntity<?> showStudentList(
            @RequestParam(required = false) Integer studentId,
            @RequestParam(required = false) Integer deptId) {

        StudentListForm studentListForm = new StudentListForm();
        studentListForm.setPage(0); // 첫 페이지는 0
        studentListForm.setStudentId(studentId);
        studentListForm.setDeptId(deptId);

        Page<Student> studentPage = studentService.readStudentList(studentListForm);

        Map<String, Object> body = new HashMap<>();
        body.put("totalPages", studentPage.getTotalPages());
        body.put("totalElements", studentPage.getTotalElements());
        body.put("currentPage", 1); // 사용자에게는 1부터 시작
        body.put("studentList", studentPage.getContent());
        body.put("deptId", deptId);
        body.put("studentId", studentId);

        return ResponseEntity.ok(body);
    }

    // 학생 조회 (페이지별)
    @GetMapping("/studentList/{page}")
    public ResponseEntity<?> showStudentListByPage(
            @PathVariable Integer page,
            @RequestParam(required = false) Integer deptId,
            @RequestParam(required = false) Integer studentId) {

        StudentListForm studentListForm = new StudentListForm();
        studentListForm.setPage(page - 1); // 0-based index
        studentListForm.setDeptId(deptId);
        studentListForm.setStudentId(studentId);

        Page<Student> studentPage = studentService.readStudentList(studentListForm);

        Map<String, Object> body = new HashMap<>();
        body.put("totalPages", studentPage.getTotalPages());
        body.put("totalElements", studentPage.getTotalElements());
        body.put("currentPage", page);
        body.put("studentList", studentPage.getContent());
        body.put("deptId", deptId);
        body.put("studentId", studentId);

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