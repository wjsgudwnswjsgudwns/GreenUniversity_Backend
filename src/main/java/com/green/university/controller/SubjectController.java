package com.green.university.controller;

import java.util.ArrayList;
import java.util.List;

import com.green.university.dto.response.SyllabusResponseDto;
import com.green.university.repository.model.SyllaBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;
import java.util.HashMap;

import com.green.university.dto.AllSubjectSearchFormDto;
import com.green.university.dto.response.ReadSyllabusDto;
import com.green.university.dto.response.SubjectDto;
import com.green.university.repository.model.Department;
import com.green.university.service.CollegeService;
import com.green.university.service.ProfessorService;
import com.green.university.service.SubjectService;

/**
 * @author 서영 
 * 강의 목록
 */

@RestController
@RequestMapping("/api/subject")
public class SubjectController {

	@Autowired
	private SubjectService subjectService;

	@Autowired
	private CollegeService collegeService;

	@Autowired
	private ProfessorService professorService;

    /**
     * 전체 과목 목록 조회 (필터링용)
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllSubjects() {
        List<SubjectDto> subjectList = subjectService.readSubjectList();
        return ResponseEntity.ok(subjectList);
    }

	// 모든 강의 조회 (모든 연도-학기에 대해서)
    @GetMapping("/list/{page}")
    public ResponseEntity<?> readSubjectList(@PathVariable Integer page) {
        List<SubjectDto> subjectList = subjectService.readSubjectList();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);
        List<SubjectDto> subjectListLimit = subjectService.readSubjectListPage(page);
        List<Department> deptList = collegeService.readDeptAll();
        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectList) {
            if (!subNameList.contains(subject.getName())) {
                subNameList.add(subject.getName());
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("subjectCount", subjectCount);
        body.put("pageCount", pageCount);
        body.put("page", page);
        body.put("subjectList", subjectListLimit);
        body.put("deptList", deptList);
        body.put("subNameList", subNameList);
        return ResponseEntity.ok(body);
    }

	// 전체 강의 목록에서 필터링
    @GetMapping("/list/search")
    public ResponseEntity<?> readSubjectListSearch(@Validated AllSubjectSearchFormDto allSubjectSearchFormDto) {

        System.out.println("검색 파라미터:");
        System.out.println("subYear: " + allSubjectSearchFormDto.getSubYear());
        System.out.println("semester: " + allSubjectSearchFormDto.getSemester());
        System.out.println("deptId: " + allSubjectSearchFormDto.getDeptId());
        System.out.println("name: " + allSubjectSearchFormDto.getName());

        List<SubjectDto> subjectList = subjectService.readSubjectListSearch(allSubjectSearchFormDto);

        System.out.println("검색 결과: " + subjectList.size() + "건");

        int subjectCount = subjectList.size();
        List<Department> deptList = collegeService.readDeptAll();
        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectService.readSubjectList()) {
            if (!subNameList.contains(subject.getName())) {
                subNameList.add(subject.getName());
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("subjectCount", subjectCount);
        body.put("subjectList", subjectList);
        body.put("deptList", deptList);
        body.put("subNameList", subNameList);
        return ResponseEntity.ok(body);
    }

	// 강의계획서 조회
    @GetMapping("/syllabus/{subjectId}")
    public ResponseEntity<?> readSyllabus(@PathVariable Integer subjectId) {
        SyllabusResponseDto readSyllabusDto = professorService.readSyllabus(subjectId);
        if (readSyllabusDto.getOverview() != null) {
            readSyllabusDto.setOverview(readSyllabusDto.getOverview().replace("\r\n", "<br>"));
        }
        if (readSyllabusDto.getObjective() != null) {
            readSyllabusDto.setObjective(readSyllabusDto.getObjective().replace("\r\n", "<br>"));
        }
        if (readSyllabusDto.getProgram() != null) {
            readSyllabusDto.setProgram(readSyllabusDto.getProgram().replace("\r\n", "<br>"));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("syllabus", readSyllabusDto);
        return ResponseEntity.ok(body);
    }

}
