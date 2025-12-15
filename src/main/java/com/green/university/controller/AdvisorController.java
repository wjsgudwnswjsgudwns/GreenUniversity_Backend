package com.green.university.controller;

import com.green.university.repository.model.Professor;
import com.green.university.repository.model.Student;
import com.green.university.service.AdvisorAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 담당 교수 배정 및 관리 API
 */
@RestController
@RequestMapping("/api/advisor")
@RequiredArgsConstructor
public class AdvisorController {

    private final AdvisorAssignmentService advisorAssignmentService;

    /**
     * 🔥 전체 학생에게 담당 교수 일괄 배정 (관리자용)
     *
     * POST /api/advisor/assign-all
     *
     * 기존 DB에 있는 모든 학생들에게 담당 교수를 자동 배정합니다.
     * - 담당 교수가 이미 있는 학생은 건너뜁니다.
     * - 학과별로 균등하게 배정합니다.
     */
    @PostMapping("/assign-all")
    public ResponseEntity<?> assignAdvisorToAllStudents() {
        try {
            Map<String, Object> result = advisorAssignmentService.assignAdvisorToAllStudents();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "전체 학생 담당 교수 배정이 완료되었습니다.");
            response.put("totalStudents", result.get("totalStudents"));
            response.put("assignedCount", result.get("assignedCount"));
            response.put("alreadyAssignedCount", result.get("alreadyAssignedCount"));
            response.put("departmentDetails", result.get("departmentDetails"));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 학과의 모든 학생에게 담당 교수 배정
     *
     * POST /api/advisor/assign/department/{deptId}
     */
    @PostMapping("/assign/department/{deptId}")
    public ResponseEntity<?> assignAdvisorsToDepartment(@PathVariable Integer deptId) {
        try {
            int assignedCount = advisorAssignmentService.assignAdvisorsToDepartment(deptId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", assignedCount + "명의 학생에게 담당 교수가 배정되었습니다.");
            response.put("assignedCount", assignedCount);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 학생에게 담당 교수 자동 배정
     *
     * POST /api/advisor/assign/{studentId}
     */
    @PostMapping("/assign/{studentId}")
    public ResponseEntity<?> assignAdvisor(@PathVariable Integer studentId) {
        try {
            Professor assignedProfessor = advisorAssignmentService.assignAdvisorToStudent(studentId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "담당 교수가 배정되었습니다.");
            response.put("studentId", studentId);
            response.put("advisorId", assignedProfessor.getId());
            response.put("advisorName", assignedProfessor.getName());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 학생의 담당 교수 수동 변경
     *
     * PUT /api/advisor/change
     */
    @PutMapping("/change")
    public ResponseEntity<?> changeAdvisor(
            @RequestParam Integer studentId,
            @RequestParam Integer professorId) {
        try {
            Student updatedStudent = advisorAssignmentService.changeAdvisor(studentId, professorId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "담당 교수가 변경되었습니다.");
            response.put("studentId", updatedStudent.getId());
            response.put("advisorId", updatedStudent.getAdvisorId());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 특정 교수의 담당 학생 목록 조회
     *
     * GET /api/advisor/professor/{professorId}/students
     */
    @GetMapping("/professor/{professorId}/students")
    public ResponseEntity<?> getAdviseeList(@PathVariable Integer professorId) {
        List<Student> advisees = advisorAssignmentService.getAdviseeList(professorId);

        Map<String, Object> response = new HashMap<>();
        response.put("professorId", professorId);
        response.put("adviseeCount", advisees.size());
        response.put("advisees", advisees);

        return ResponseEntity.ok(response);
    }

    /**
     * 특정 교수의 담당 학생 수 조회
     *
     * GET /api/advisor/count/{professorId}
     */
    @GetMapping("/count/{professorId}")
    public ResponseEntity<?> getAdviseeCount(@PathVariable Integer professorId) {
        long count = advisorAssignmentService.getAdviseeCount(professorId);

        Map<String, Object> response = new HashMap<>();
        response.put("professorId", professorId);
        response.put("adviseeCount", count);

        return ResponseEntity.ok(response);
    }

    /**
     * 학과별 교수-학생 배정 현황 조회
     *
     * GET /api/advisor/department/{deptId}/status
     */
    @GetMapping("/department/{deptId}/status")
    public ResponseEntity<?> getDepartmentAdvisorStatus(@PathVariable Integer deptId) {
        Map<String, Object> status = advisorAssignmentService.getDepartmentAdvisorStatus(deptId);

        return ResponseEntity.ok(status);
    }

    /**
     * 전체 학과 담당 교수 배정 현황 조회 (관리자용)
     *
     * GET /api/advisor/status/all
     */
    @GetMapping("/status/all")
    public ResponseEntity<?> getAllAdvisorStatus() {
        List<Map<String, Object>> statusList = advisorAssignmentService.getAllAdvisorStatus();

        Map<String, Object> response = new HashMap<>();
        response.put("departments", statusList);

        return ResponseEntity.ok(response);
    }
}