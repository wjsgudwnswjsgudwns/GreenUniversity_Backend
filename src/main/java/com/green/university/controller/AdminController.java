package com.green.university.controller;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
// Model import removed as REST controllers return JSON instead of views
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.green.university.dto.CollTuitFormDto;
import com.green.university.dto.CollegeFormDto;
import com.green.university.dto.DepartmentFormDto;
import com.green.university.dto.RoomFormDto;
import com.green.university.dto.SubjectFormDto;
import com.green.university.repository.model.College;
import com.green.university.repository.model.Department;
import com.green.university.repository.model.Room;
import com.green.university.repository.model.Subject;
import com.green.university.service.AdminService;

/**
 * 
 * @author 박성희 
 * Admin 수업 조회/입력 관련 Controller
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
	@Autowired
	private AdminService adminService;

	// 단과대 페이지
    @GetMapping("/college")
    public ResponseEntity<Map<String, Object>> college(@RequestParam(defaultValue = "select") String crud) {
        List<College> collegeList = adminService.readCollege();
        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        return ResponseEntity.ok(res);
    }

	// 단과대학 입력 기능
    @PostMapping("/college")
    public ResponseEntity<?> collegeProc(@RequestBody CollegeFormDto collegeFormDto) {
        adminService.createCollege(collegeFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	// 단과대학 삭제 기능
    @GetMapping("/collegeDelete")
    public ResponseEntity<?> deleteCollege(@RequestParam Integer id) {
        adminService.deleteCollege(id);
        return ResponseEntity.ok().build();
    }

	// 학과 페이지 페이지 이동 시, 단과대학 조회 후 이동
    @GetMapping("/department")
    public ResponseEntity<Map<String, Object>> department(@RequestParam(defaultValue = "select") String crud) {
        List<Department> departmentList = adminService.readDepartment();
        List<College> collegeList = adminService.readCollege();
        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        res.put("departmentList", departmentList.isEmpty() ? null : departmentList);
        return ResponseEntity.ok(res);
    }

	// 학과 입력 기능
    @PostMapping("/department")
    public ResponseEntity<?> departmentProc(@RequestBody DepartmentFormDto departmentFormDto) {
        adminService.createDepartment(departmentFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	// 학과 삭제 기능
    @GetMapping("/departmentDelete")
    public ResponseEntity<?> deleteDepartment(@RequestParam Integer id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }

	// 학과 수정 기능
    @PutMapping("/department")
    public ResponseEntity<?> updateDepartment(@RequestBody DepartmentFormDto departmentFormDto) {
        adminService.updateDepartment(departmentFormDto);
        return ResponseEntity.ok().build();
    }

	// 강의실 페이지
    @GetMapping("/room")
    public ResponseEntity<Map<String, Object>> room(@RequestParam(defaultValue = "select") String crud) {
        List<Room> roomList = adminService.readRoom();
        List<College> collegeList = adminService.readCollege();
        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        res.put("roomList", roomList.isEmpty() ? null : roomList);
        return ResponseEntity.ok(res);
    }

	// 강의실 입력 기능
    @PostMapping("/room")
    public ResponseEntity<?> roomProc(@RequestBody RoomFormDto roomFormDto) {
        adminService.createRoom(roomFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	// 강의실 삭제 기능
    @GetMapping("/roomDelete")
    public ResponseEntity<?> deleteRoom(@RequestParam String id) {
        adminService.deleteRoom(id);
        return ResponseEntity.ok().build();
    }

	// 강의 페이지
    @GetMapping("/subject")
    public ResponseEntity<Map<String, Object>> subject(@RequestParam(defaultValue = "select") String crud) {
        List<Subject> subjectList = adminService.readSubject();
        List<College> collegeList = adminService.readCollege();
        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        res.put("subjectList", subjectList.isEmpty() ? null : subjectList);
        return ResponseEntity.ok(res);
    }

	// 강의 입력 기능
    @PostMapping("/subject")
    public ResponseEntity<?> insertSubject(@RequestBody SubjectFormDto subjectFormDto) {
        adminService.createSubjectAndSyllabus(subjectFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	// 강의 삭제 기능
    @GetMapping("/subjectDelete")
    public ResponseEntity<?> deleteSubject(@RequestParam Integer id) {
        adminService.deleteSubject(id);
        return ResponseEntity.ok().build();
    }

	// 강의 수정 기능
    @PutMapping("/subject")
    public ResponseEntity<?> updateSubject(@RequestBody SubjectFormDto subjectFormDto) {
        adminService.updateSubject(subjectFormDto);
        return ResponseEntity.ok().build();
    }

	// 단과대별 등록금 페이지
    @GetMapping("/tuition")
    public ResponseEntity<Map<String, Object>> collTuit(@RequestParam(defaultValue = "select") String crud) {
        List<CollTuitFormDto> collTuitList = adminService.readCollTuit();
        List<College> collegeList = adminService.readCollege();
        Map<String, Object> res = new HashMap<>();
        res.put("crud", crud);
        res.put("collegeList", collegeList.isEmpty() ? null : collegeList);
        res.put("collTuitList", collTuitList.isEmpty() ? null : collTuitList);
        return ResponseEntity.ok(res);
    }

	// 단과대별 등록금 입력 기능
    @PostMapping("/tuition")
    public ResponseEntity<?> insertcollTuit(@RequestBody CollTuitFormDto collTuitFormDto) {
        adminService.createCollTuit(collTuitFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	// 단과대 등록금 삭제 기능
    @GetMapping("/tuitionDelete")
    public ResponseEntity<?> deleteCollTuit(@RequestParam Integer collegeId) {
        adminService.deleteCollTuit(collegeId);
        return ResponseEntity.ok().build();
    }

	// 단과대 등록금 수정 기능
    @PutMapping("/tuitionUpdate")
    public ResponseEntity<?> updateCollTuit(@RequestBody CollTuitFormDto collTuitFormDto) {
        adminService.updateCollTuit(collTuitFormDto);
        return ResponseEntity.ok().build();
    }

}
