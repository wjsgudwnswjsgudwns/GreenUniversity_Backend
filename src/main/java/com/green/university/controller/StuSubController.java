package com.green.university.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import jakarta.servlet.http.HttpSession;

import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.green.university.dto.CurrentSemesterSubjectSearchFormDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.StuSubAppDto;
import com.green.university.dto.response.SubjectDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.BreakAppService;
import com.green.university.service.CollegeService;
import com.green.university.service.PreStuSubService;
import com.green.university.service.StuStatService;
import com.green.university.service.StuSubService;
import com.green.university.service.SubjectService;
import com.green.university.service.UserService;
import com.green.university.utils.Define;
import com.green.university.utils.StuStatUtil;

/**
 * @author 서영 
 * 수강 신청 관련 (preStuSub 포함) 강의 시간표는 SubjectController 대신 일부러 여기에 넣음
 */

@RestController
@RequestMapping("/api/sugang")
public class StuSubController {

	@Autowired
	private HttpSession session;

	@Autowired
	private SubjectService subjectService;

    @Autowired
    private SubjectJpaRepository subjectJpaRepository;

	@Autowired
	private CollegeService collegeService;

	@Autowired
	private PreStuSubService preStuSubService;

	@Autowired
	private StuSubService stuSubService;

	@Autowired
	private StuStatService stuStatService;

	@Autowired
	private BreakAppService breakAppService;

	@Autowired
	private UserService userService;

	// 예비 수강신청 기간 : 0, 수강신청 기간 : 1, 수강신청 기간 종료 : 2
	public static int SUGANG_PERIOD = 0;

    // 예비 수강신청 기간에서 수강신청 기간으로 변경하는 페이지 (교직원용)
//    @GetMapping("/period")
//    public ResponseEntity<?> updatePeriod() {
//        Map<String, Object> body = new HashMap<>();
//        body.put("period", SUGANG_PERIOD);
//        body.put("message", "예비 수강신청 기간 설정 페이지입니다.");
//        return ResponseEntity.ok(body);
//    }

	// 예비 수강 신청 기간 -> 수강 신청 기간
//    @GetMapping("/updatePeriod1")
//    public ResponseEntity<?> updatePeriodProc1() {
//        SUGANG_PERIOD = 1;
//        stuSubService.createStuSubByPreStuSub();
//        Map<String, Object> body = new HashMap<>();
//        body.put("period", SUGANG_PERIOD);
//        body.put("message", "수강신청 기간으로 변경되었습니다.");
//        return ResponseEntity.ok(body);
//    }

	// 수강 신청 기간 -> 종료
//    @GetMapping("/updatePeriod2")
//    public ResponseEntity<?> updatePeriodProc2() {
//        SUGANG_PERIOD = 2;
//        Map<String, Object> body = new HashMap<>();
//        body.put("period", SUGANG_PERIOD);
//        body.put("message", "수강신청 기간이 종료되었습니다.");
//        return ResponseEntity.ok(body);
//    }

    // 과목 조회 (현재 학기)
    @GetMapping("/subjectList/{page}")
    public ResponseEntity<?> readSubjectList(@PathVariable Integer page) {
        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);
        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage((page - 1) * 20);
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

    // 과목 조회 (현재 학기)에서 필터링
    @GetMapping("/subjectList/search")
    public ResponseEntity<?> readSubjectListSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto) {
        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);
        int subjectCount = subjectList.size();
        List<Department> deptList = collegeService.readDeptAll();
        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectService.readSubjectListByCurrentSemester()) {
            if (!subNameList.contains(subject.getName())) {
                subNameList.add(subject.getName());
            }
        }
        Map<String, Object> body = new HashMap<>();
        body.put("subjectList", subjectList);
        body.put("subjectCount", subjectCount);
        body.put("deptList", deptList);
        body.put("subNameList", subNameList);
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 목록을 조회한다. 페이지 단위로 분리하여 현재 담겨있는 과목 여부를 포함한다.
     *
     * @param page 현재 페이지 번호 (1부터 시작)
     * @return 예비 수강 신청 가능한 과목 목록과 부가 정보를 담은 JSON
     */
    @GetMapping("/pre/{page}")
    public ResponseEntity<?> preStuSubApplication(@PathVariable Integer page) {
        // 예비 수강 신청 기간이 아니라면 오류를 발생시킨다.
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        // 이번 학기에 재학 상태가 되지 않는 학생이라면 진입 불가
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());

        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId()); // 최근 순으로 정렬되어 있음
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        // 전체 강의 리스트 및 페이징
        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);

        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage((page - 1) * 20);
        // 각 강의가 이미 예비 수강 신청되었는지 여부를 표시
        for (SubjectDto sub : subjectListLimit) {
            PreStuSub preStuSub = preStuSubService.readPreStuSub(principal.getId(), sub.getId());
            sub.setStatus(preStuSub != null);
        }

        // 필터에 사용할 학과 및 강의명 목록 (중복 제거)
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

    /**
     * 예비 수강 신청 처리 (신청)
     *
     * @param subjectId 예비 수강신청 대상 과목 ID
     * @return 신청 완료 메시지
     */
    @PostMapping("/pre/{subjectId}")
    public ResponseEntity<?> insertPreStuSubAppProc(@PathVariable Integer subjectId) {
        // 예비 수강 신청 기간이 아니라면 오류를 발생시킨다.
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        Integer studentId = ((PrincipalDto) session.getAttribute(Define.PRINCIPAL)).getId();
        preStuSubService.createPreStuSub(studentId, subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "예비 수강 신청이 완료되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 처리 (취소)
     *
     * @param subjectId 예비 수강신청 취소 대상 과목 ID
     * @param type 호출 위치를 구분하기 위한 파라미터 (0: 검색 페이지, 1: 내역 페이지)
     * @return 취소 완료 메시지와 호출 타입
     */
    @DeleteMapping("/pre/{subjectId}")
    public ResponseEntity<?> deletePreStuSubAppProc(@PathVariable Integer subjectId,
            @RequestParam Integer type) {
        // 예비 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        Integer studentId = ((PrincipalDto) session.getAttribute(Define.PRINCIPAL)).getId();
        preStuSubService.deletePreStuSub(studentId, subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "예비 수강 신청이 취소되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    // 예비 수강 신청 강의 목록에서 필터링
    @GetMapping("/pre/search")
    public ResponseEntity<?> preStuSubApplicationSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto) {
        // 예비 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        // 필터링 된 강의 리스트를 가져옴
        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);
        // 각 강의에 대해 예비 수강 신청 여부 설정
        for (SubjectDto sub : subjectList) {
            PreStuSub preStuSub = preStuSubService.readPreStuSub(principal.getId(), sub.getId());
            sub.setStatus(preStuSub != null);
        }
        int subjectCount = subjectList.size();
        // 필터에 사용할 전체 학과 정보
        List<Department> deptList = collegeService.readDeptAll();
        // 필터에 사용할 강의 이름 정보 (중복 값 제거)
        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectService.readSubjectListByCurrentSemester()) {
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

    /**
     * 수강 신청 페이지 정보를 반환한다. 현재 수강신청 기간에 신청 가능한 과목 목록과 상태를 제공한다.
     *
     * @param page 페이지 번호 (1부터 시작)
     * @return 수강 신청 가능한 과목 목록과 페이징 정보 등이 담긴 JSON
     */
    @GetMapping("/application/{page}")
    public ResponseEntity<?> stuSubApplication(@PathVariable Integer page) {
        // 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        // 학적 상태 확인
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        // 강의 리스트 및 페이징
        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);
        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage((page - 1) * 20);
        // 각 강의에 대해 수강신청 여부를 설정
        for (SubjectDto sub : subjectListLimit) {
            StuSub stuSub = stuSubService.readStuSub(principal.getId(), sub.getId());
            sub.setStatus(stuSub != null);
        }
        // 필터에 사용할 학과 및 강의명 정보
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

    // 수강 신청 강의 목록에서 필터링
    @GetMapping("/application/search")
    public ResponseEntity<?> stuSubApplicationSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto) {
        // 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        // 필터링된 강의 리스트를 가져옴
        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);
        // 각 강의에 대해 수강신청 여부를 설정
        for (SubjectDto sub : subjectList) {
            StuSub stuSub = stuSubService.readStuSub(principal.getId(), sub.getId());
            sub.setStatus(stuSub != null);
        }
        int subjectCount = subjectList.size();
        // 필터에 사용할 학과 및 강의명 목록
        List<Department> deptList = collegeService.readDeptAll();
        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectList) {
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

    /**
     * 수강 신청 처리 (신청)
     *
     * @param subjectId 신청할 과목의 ID
     * @param type 호출 위치를 구분하기 위한 파라미터 (0: 검색 페이지, 1: 예비 수강신청 내역)
     * @return 신청 완료 메시지와 호출 타입
     */
    @PostMapping("/insertApp/{subjectId}")
    public ResponseEntity<?> insertStuSubAppProc(@PathVariable Integer subjectId, @RequestParam Integer type) {
        // 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        Integer studentId = ((PrincipalDto) session.getAttribute(Define.PRINCIPAL)).getId();
        stuSubService.createStuSub(studentId, subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "수강 신청이 완료되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    /**
     * 수강 신청 처리 (취소)
     *
     * @param subjectId 취소할 과목의 ID
     * @param type 호출 위치를 구분하기 위한 파라미터 (0: 검색 페이지, 1: 예비 수강신청 내역)
     * @return 취소 완료 메시지와 호출 타입
     */
    @DeleteMapping("/deleteApp/{subjectId}")
    public ResponseEntity<?> deleteStuSubAppProc(@PathVariable Integer subjectId, @RequestParam Integer type) {
        // 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        Integer studentId = ((PrincipalDto) session.getAttribute(Define.PRINCIPAL)).getId();
        stuSubService.deleteStuSub(studentId, subjectId);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "수강 신청이 취소되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 및 수강 신청 내역을 조회한다. type 값에 따라 예비 기간 또는 신청 기간 데이터를 반환한다.
     *
     * @param type 0: 예비 수강신청 기간, 1: 수강신청 기간
     * @return 신청 내역과 총 학점 등을 담은 JSON
     */
    @GetMapping("/preAppList")
    public ResponseEntity<?> preStuSubAppList(@RequestParam Integer type) {
        // 이번 학기에 재학 상태가 되지 않는 학생이라면 진입 불가
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        Map<String, Object> body = new HashMap<>();
        body.put("type", type);

        // 예비 수강 신청 기간에 조회 시
        if (type == 0) {
            List<PreStuSub> preStuSubList = preStuSubService.readPreStuSubList(principal.getId());

            int sumGrades = preStuSubList.stream()
                    .mapToInt(ps -> {
                        Subject subject = subjectJpaRepository.findById(ps.getSubjectId()).orElse(null);
                        return subject != null ? subject.getGrades() : 0;
                    })
                    .sum();

            body.put("stuSubList", preStuSubList);
            body.put("sumGrades", sumGrades);
            return ResponseEntity.ok(body);
        }

        // 수강 신청 기간에 조회 시, 예비 수강신청 목록에서 수강신청으로 자동 이동된 강의와 직접 신청해야 하는 강의를 분리해 반환한다.
        // 수강 신청 기간이 아니라면 오류 발생
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        // 수강 신청이 완료되지 않은 예비 수강 신청 내역
        List<PreStuSub> preStuSubList1 = stuSubService.readPreStuSubByStuSub(principal.getId());
        // 수강 신청 내역
        List<StuSub> stuSubList = stuSubService.readStuSubList(principal.getId());
        int sumGrades = stuSubList.stream()
                .mapToInt(s -> s.getSubject() != null ? s.getSubject().getGrades() : 0)
                .sum();
        body.put("preStuSubList", preStuSubList1);
        body.put("stuSubList", stuSubList);
        body.put("sumGrades", sumGrades);
        return ResponseEntity.ok(body);
    }

    /**
     * 수강 신청 내역을 조회한다.
     *
     * @return 수강 신청한 과목 목록과 총 학점을 포함한 JSON
     */
    @GetMapping("/list")
    public ResponseEntity<?> stuSubAppList() {
        // 예비 수강 신청 기간이라면 수강 신청 내역을 볼 수 없다.
        if (SUGANG_PERIOD == 0) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }
        // 학적 상태 확인
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        Student studentInfo = userService.readStudent(principal.getId());
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);
        List<StuSub> stuSubList = stuSubService.readStuSubList(principal.getId());
        int sumGrades = stuSubList.stream()
                .mapToInt(s -> s.getSubject() != null ? s.getSubject().getGrades() : 0)
                .sum();
        Map<String, Object> body = new HashMap<>();
        body.put("stuSubList", stuSubList);
        body.put("sumGrades", sumGrades);
        return ResponseEntity.ok(body);
    }

}
