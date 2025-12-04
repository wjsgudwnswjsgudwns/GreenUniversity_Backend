package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.Map;
import java.util.HashMap;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.BreakApp;
import com.green.university.repository.model.StuStat;
import com.green.university.repository.model.Student;
import com.green.university.repository.model.Tuition;
import com.green.university.service.BreakAppService;
import com.green.university.service.CollegeService;
import com.green.university.service.StuStatService;
import com.green.university.service.TuitionService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;
import com.green.university.utils.StuStatUtil;

/**
 * 
 * @author 서영 
 * 등록금, 장학금 관련
 *
 */

@RestController
@RequestMapping("/api/tuition")
public class TuitionController {

	@Autowired
	private HttpSession session;

	@Autowired
	private TuitionService tuitionService;

	@Autowired
	private StuStatService stuStatService;

	@Autowired
	private UserService userService;

	@Autowired
	private CollegeService collegeService;

	@Autowired
	private BreakAppService breakAppService;

	/**
	 * @return 납부된 등록금 내역 조회 페이지
	 */
    @GetMapping("/list")
    public ResponseEntity<?> tuitionList() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        List<Tuition> tuitionList = tuitionService.readTuitionListByStatus(principal.getId(), true);
        Map<String, Object> body = new HashMap<>();
        body.put("tuitionList", tuitionList);
        return ResponseEntity.ok(body);
    }

	/**
	 * @return 등록금 납부 고지서 조회 페이지
	 * 
	 *         해당 학기 (2023-1)에 등록금을 납부한 기록이 있다면 납부하기 버튼 제거
	 */
    @GetMapping("/payment")
    public ResponseEntity<?> tuitionPayment() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("등록금", stuStatEntity, breakAppList);
        String deptName = collegeService.readDeptById(studentInfo.getDeptId()).getName();
        String collName = collegeService
                .readCollById(collegeService.readDeptById(studentInfo.getDeptId()).getId()).getName();
        Tuition tuitionEntity = tuitionService.readByStudentIdAndSemester(principal.getId(), Define.CURRENT_YEAR,
                Define.CURRENT_SEMESTER);
        if (tuitionEntity == null) {
            throw new CustomRestfullException("등록금 납부 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("student", studentInfo);
        body.put("deptName", deptName);
        body.put("collName", collName);
        body.put("tuition", tuitionEntity);
        return ResponseEntity.ok(body);
    }

	/**
	 * 등록금 납부
	 * 
	 * @return 등록금 납부 페이지로 다시 돌아가서 납부 완료됨을 보여주기
	 */
    @PostMapping("/payment")
    public ResponseEntity<Map<String, String>> tuitionPaymentProc() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        tuitionService.updateStatus(principal.getId());
        Map<String, String> body = new HashMap<>();
        body.put("message", "등록금이 납부되었습니다.");
        return ResponseEntity.ok(body);
    }

	/**
	 * 장학금 유형 설정 + 등록금 납부 고지서 생성 페이지
	 */
    @GetMapping("/bill")
    public ResponseEntity<Map<String, String>> createPayment() {
        Map<String, String> body = new HashMap<>();
        body.put("message", "등록금 납부 고지서 생성 페이지입니다.");
        return ResponseEntity.ok(body);
    }

	/**
	 * 등록금 납부 고지서 생성
	 */
    @GetMapping("/create")
    public ResponseEntity<?> createTuiProc() {
        List<Integer> studentIdList = stuStatService.readIdList();
        int insertCount = 0;
        for (Integer studentId : studentIdList) {
            insertCount += tuitionService.createTuition(studentId);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("insertCount", insertCount);
        return ResponseEntity.ok(body);
    }

}
