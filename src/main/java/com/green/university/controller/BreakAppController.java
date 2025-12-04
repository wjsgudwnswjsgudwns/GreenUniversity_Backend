package com.green.university.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
// Model import removed as REST controllers return JSON
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.green.university.dto.BreakAppFormDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.BreakApp;
import com.green.university.repository.model.Student;
import com.green.university.service.BreakAppService;
import com.green.university.service.CollegeService;
import com.green.university.service.StuStatService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;

/**
 * @author 서영 
 * 휴학 신청 관련 컨트롤러
 */
@RestController
@RequestMapping("/api/break")
public class BreakAppController {

	@Autowired
	private HttpSession session;

	@Autowired
	private BreakAppService breakAppService;

	@Autowired
	private StuStatService stuStatService;

	@Autowired
	private UserService userService;

	@Autowired
	private CollegeService collegeService;

	/**
	 * @return 휴학 신청 페이지
	 */
    @GetMapping("/application")
    public ResponseEntity<Map<String, Object>> breakApplication() {

        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());

        // 학생이 재학 상태가 아니라면 신청 불가능
        if (!stuStatService.readCurrentStatus(principal.getId()).getStatus().equals("재학")) {
            throw new CustomRestfullException("휴학 신청 대상이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        List<BreakApp> breakList = breakAppService.readByStudentId(principal.getId());
        if (!breakList.isEmpty()) {
            if (breakList.get(0).getFromYear() == Define.CURRENT_YEAR
                    && breakList.get(0).getFromSemester() == Define.CURRENT_SEMESTER
                    && !breakList.get(0).getStatus().equals("반려")) {
                throw new CustomRestfullException("이미 휴학 신청 내역이 존재합니다.", HttpStatus.BAD_REQUEST);
            }
        }

        Map<String, Object> res = new HashMap<>();
        res.put("student", studentInfo);
        // 학과 및 단과대 이름 계산
        String deptName = collegeService.readDeptById(studentInfo.getDeptId()).getName();
        String collName = collegeService
                .readCollById(collegeService.readDeptById(studentInfo.getDeptId()).getId()).getName();
        res.put("deptName", deptName);
        res.put("collName", collName);
        return ResponseEntity.ok(res);
    }

	/**
	 * 휴복학 신청 (신청하면 교직원이 확인해서 승인하면 학적 변동)
	 * 
	 * @return 휴복학 신청 내역 페이지
	 */
    @PostMapping("/application")
    public ResponseEntity<?> breakApplicationProc(@Validated @RequestBody BreakAppFormDto breakAppFormDto) {

		PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);

		// 선택한 종료 연도-학기가 시작 연도-학기보다 이전이라면 신청 불가능
		// ex) 시작 연도-학기 : 2023-2 / 종료 연도-학기 2023-1
		if (Define.CURRENT_YEAR == breakAppFormDto.getToYear()
				&& Define.CURRENT_SEMESTER > breakAppFormDto.getToSemester()) {
			throw new CustomRestfullException("종료 학기가 시작 학기 이전입니다.", HttpStatus.BAD_REQUEST);
		}
		breakAppFormDto.setStudentId(principal.getId());
		breakAppFormDto.setFromYear(Define.CURRENT_YEAR);
		breakAppFormDto.setFromSemester(Define.CURRENT_SEMESTER);

        breakAppService.createBreakApp(breakAppFormDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

	/**
	 * @return 휴복학 신청 내역 페이지 (학생용)
	 */
    @GetMapping("/list")
    public ResponseEntity<List<BreakApp>> breakAppListByStudentId() {

        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<BreakApp> breakAppList = breakAppService.readByStudentId(principal.getId());
        return ResponseEntity.ok(breakAppList);
    }

	/**
	 * @return 처리되지 않은 휴복학 신청 내역 페이지 (교직원용)
	 */
    @GetMapping("/list/staff")
    public ResponseEntity<List<BreakApp>> breakAppListByState() {

        List<BreakApp> breakAppList = breakAppService.readByStatus("처리중");
        return ResponseEntity.ok(breakAppList);
    }

	/**
	 * @return 휴학 신청서 확인 학생 / 교직원에 따라 옆에 카테고리 바뀌어야 함
	 */
    @GetMapping("/detail/{id}")
    public ResponseEntity<Map<String, Object>> breakDetail(@PathVariable Integer id) {

        BreakApp breakApp = breakAppService.readById(id);
        Student studentInfo = userService.readStudent(breakApp.getStudentId());
        String deptName = collegeService.readDeptById(studentInfo.getDeptId()).getName();
        String collName = collegeService
                .readCollById(collegeService.readDeptById(studentInfo.getDeptId()).getId()).getName();
        Map<String, Object> res = new HashMap<>();
        res.put("breakApp", breakApp);
        res.put("student", studentInfo);
        res.put("deptName", deptName);
        res.put("collName", collName);
        return ResponseEntity.ok(res);
    }

	/**
	 * 휴학 신청 취소 (학생)
	 */
    @PostMapping("/delete/{id}")
    public ResponseEntity<?> deleteBreakApp(@PathVariable Integer id) {

		// 신청서의 학번과 현재 로그인된 아이디가 일치하는지 확인
		PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
		if (breakAppService.readById(id).getStudentId().equals(principal.getId()) == false) {
			throw new CustomRestfullException("해당 신청자만 신청을 취소할 수 있습니다.", HttpStatus.UNAUTHORIZED);
		}

        breakAppService.deleteById(id);
        return ResponseEntity.ok().build();
    }

	/**
	 * 휴학 신청 처리 (교직원)
	 */
    @PostMapping("/update/{id}")
    public ResponseEntity<?> updateBreakApp(@PathVariable Integer id, @RequestParam String status) {
        breakAppService.updateById(id, status);
        return ResponseEntity.ok().build();
    }

}
