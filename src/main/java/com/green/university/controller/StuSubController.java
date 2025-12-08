package com.green.university.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.dto.response.StuSubAppDto;
import com.green.university.repository.SubjectJpaRepository;
import com.green.university.repository.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import com.green.university.dto.CurrentSemesterSubjectSearchFormDto;
import com.green.university.dto.response.SubjectDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.*;
import com.green.university.utils.StuStatUtil;

/**
 * 수강 신청 관련 REST API Controller (JWT 기반)
 */
@RestController
@RequestMapping("/api/sugang")
public class StuSubController {

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

    // 예비 수강신청 기간: 0, 수강신청 기간: 1, 수강신청 기간 종료: 2
    public static int SUGANG_PERIOD = 0;

    /**
     * Authentication에서 학생 ID 추출
     */
    private Integer getStudentId(Authentication authentication) {
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        return principal.getId();
    }

    /**
     * [수정됨] Subject 엔티티를 StuSubAppDto로 변환하는 헬퍼 메서드
     */
    private StuSubAppDto convertToDto(Subject subject) {
        StuSubAppDto dto = new StuSubAppDto();
        dto.setSubjectId(subject.getId());
        dto.setSubjectName(subject.getName());
        dto.setGrades(subject.getGrades());
        dto.setSubDay(subject.getSubDay());
        dto.setStartTime(subject.getStartTime());
        dto.setEndTime(subject.getEndTime());
        dto.setNumOfStudent(subject.getNumOfStudent());
        dto.setCapacity(subject.getCapacity());

        // Null Check: 교수 정보
        if (subject.getProfessor() != null) {
            dto.setProfessorName(subject.getProfessor().getName());
        }

        // Null Check: 강의실 정보
        if (subject.getRoom() != null) {
            dto.setRoomId(subject.getRoom().getId());
        }

        return dto;
    }



    /**
     * 과목 조회 (현재 학기)
     */
    @GetMapping("/subjectList/{page}")
    public ResponseEntity<?> readSubjectList(@PathVariable Integer page) {
        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);

        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage(page);

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
     * 과목 조회 (현재 학기)에서 필터링
     */
    @GetMapping("/subjectList/search")
    public ResponseEntity<?> readSubjectListSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto) {

        System.out.println("검색 요청 받음:");
        System.out.println("type: " + currentSemesterSubjectSearchFormDto.getType());
        System.out.println("deptId: " + currentSemesterSubjectSearchFormDto.getDeptId());
        System.out.println("name: " + currentSemesterSubjectSearchFormDto.getName());

        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);

        System.out.println("검색 결과 개수: " + subjectList.size());

        int subjectCount = subjectList.size();
        List<Department> deptList = collegeService.readDeptAll();

        List<String> subNameList = new ArrayList<>();
        for (SubjectDto subject : subjectList) {
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
     * 예비 수강 신청 목록 조회 (페이징)
     */
    @GetMapping("/pre/{page}")
    public ResponseEntity<?> preStuSubApplication(@PathVariable Integer page, Authentication authentication) {
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);

        Student studentInfo = userService.readStudent(studentId);
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);

        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage(page);

        for (SubjectDto sub : subjectListLimit) {
            PreStuSub preStuSub = preStuSubService.readPreStuSub(studentId, sub.getId());
            sub.setStatus(preStuSub != null);
        }

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
     */
    @PostMapping("/pre/{subjectId}")
    public ResponseEntity<?> insertPreStuSubAppProc(@PathVariable Integer subjectId, Authentication authentication) {
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);
        preStuSubService.createPreStuSub(studentId, subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "예비 수강 신청이 완료되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 처리 (취소)
     */
    @DeleteMapping("/pre/{subjectId}")
    public ResponseEntity<?> deletePreStuSubAppProc(@PathVariable Integer subjectId,
                                                    @RequestParam Integer type, Authentication authentication) {
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);
        preStuSubService.deletePreStuSub(studentId, subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "예비 수강 신청이 취소되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 강의 목록에서 필터링
     */
    @GetMapping("/pre/search")
    public ResponseEntity<?> preStuSubApplicationSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto,
            Authentication authentication) {
        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);

        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);

        for (SubjectDto sub : subjectList) {
            PreStuSub preStuSub = preStuSubService.readPreStuSub(studentId, sub.getId());
            sub.setStatus(preStuSub != null);
        }

        int subjectCount = subjectList.size();
        List<Department> deptList = collegeService.readDeptAll();
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
     * 수강 신청 페이지 정보 반환
     */
    @GetMapping("/application/{page}")
    public ResponseEntity<?> stuSubApplication(@PathVariable Integer page, Authentication authentication) {
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);

        Student studentInfo = userService.readStudent(studentId);
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        List<SubjectDto> subjectList = subjectService.readSubjectListByCurrentSemester();
        int subjectCount = subjectList.size();
        int pageCount = (int) Math.ceil(subjectCount / 20.0);

        List<SubjectDto> subjectListLimit = subjectService.readSubjectListByCurrentSemesterPage(page);

        for (SubjectDto sub : subjectListLimit) {
            StuSub stuSub = stuSubService.readStuSub(studentId, sub.getId());
            sub.setStatus(stuSub != null);
        }

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
     * 수강 신청 강의 목록에서 필터링
     */
    @GetMapping("/application/search")
    public ResponseEntity<?> stuSubApplicationSearch(
            @Validated CurrentSemesterSubjectSearchFormDto currentSemesterSubjectSearchFormDto,
            Authentication authentication) {
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);

        List<SubjectDto> subjectList = subjectService
                .readSubjectListSearchByCurrentSemester(currentSemesterSubjectSearchFormDto);

        for (SubjectDto sub : subjectList) {
            StuSub stuSub = stuSubService.readStuSub(studentId, sub.getId());
            sub.setStatus(stuSub != null);
        }

        int subjectCount = subjectList.size();
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
     */
    @PostMapping("/insertApp/{subjectId}")
    public ResponseEntity<?> insertStuSubAppProc(@PathVariable Integer subjectId,
                                                 @RequestParam Integer type, Authentication authentication) {
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);
        stuSubService.createStuSub(studentId, subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "수강 신청이 완료되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    /**
     * 수강 신청 처리 (취소)
     */
    @DeleteMapping("/deleteApp/{subjectId}")
    public ResponseEntity<?> deleteStuSubAppProc(@PathVariable Integer subjectId,
                                                 @RequestParam Integer type, Authentication authentication) {
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);
        stuSubService.deleteStuSub(studentId, subjectId);

        Map<String, Object> body = new HashMap<>();
        body.put("message", "수강 신청이 취소되었습니다.");
        body.put("type", type);
        return ResponseEntity.ok(body);
    }

    /**
     * 예비 수강 신청 및 수강 신청 내역 조회
     */
    @GetMapping("/preAppList")
    public ResponseEntity<?> preStuSubAppList(@RequestParam Integer type, Authentication authentication) {
        Integer studentId = getStudentId(authentication);

        // 학적 상태 확인
        Student studentInfo = userService.readStudent(studentId);
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        Map<String, Object> body = new HashMap<>();
        body.put("type", type);

        // type 0: 예비 수강 신청 기간 조회
        if (type == 0) {
            List<PreStuSub> preStuSubList = preStuSubService.readPreStuSubList(studentId);

            // PreStuSub 리스트 -> StuSubAppDto 리스트로 변환
            List<StuSubAppDto> dtoList = new ArrayList<>();
            int sumGrades = 0;

            for (PreStuSub pss : preStuSubList) {
                Subject subject = subjectJpaRepository.findById(pss.getSubjectId()).orElse(null);
                if (subject != null) {
                    dtoList.add(convertToDto(subject)); // DTO 변환
                    sumGrades += subject.getGrades();
                }
            }

            body.put("stuSubList", dtoList); // 변환된 DTO 리스트 전달
            body.put("sumGrades", sumGrades);
            return ResponseEntity.ok(body);
        }

        // type 1: 본 수강 신청 기간 조회
        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        // 1. 신청 미완료 목록 (예비 수강 신청 내역 중 실제 수강 신청 안 한 것)
        List<PreStuSub> preStuSubList1 = stuSubService.readPreStuSubByStuSub(studentId);
        List<StuSubAppDto> preDtoList = new ArrayList<>();

        for (PreStuSub pss : preStuSubList1) {
            Subject subject = subjectJpaRepository.findById(pss.getSubjectId()).orElse(null);
            if (subject != null) {
                preDtoList.add(convertToDto(subject));
            }
        }

        // 2. 신청 완료 목록 (StuSub)
        List<StuSub> stuSubList = stuSubService.readStuSubList(studentId);
        List<StuSubAppDto> completedDtoList = new ArrayList<>();
        int sumGrades = 0;

        for (StuSub ss : stuSubList) {
            if (ss.getSubject() != null) {
                completedDtoList.add(convertToDto(ss.getSubject()));
                sumGrades += ss.getSubject().getGrades();
            }
        }

        body.put("preStuSubList", preDtoList);     // DTO 리스트 (미완료)
        body.put("stuSubList", completedDtoList);  // DTO 리스트 (완료)
        body.put("sumGrades", sumGrades);
        return ResponseEntity.ok(body);
    }

    /**
     * 수강 신청 내역 조회
     */
    @GetMapping("/list")
    public ResponseEntity<?> stuSubAppList(Authentication authentication) {
        if (SUGANG_PERIOD == 0) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        Integer studentId = getStudentId(authentication);

        // 학적 상태 확인
        Student studentInfo = userService.readStudent(studentId);
        StuStat stuStatEntity = stuStatService.readCurrentStatus(studentInfo.getId());
        List<BreakApp> breakAppList = breakAppService.readByStudentId(studentInfo.getId());
        StuStatUtil.checkStuStat("수강신청", stuStatEntity, breakAppList);

        List<StuSub> stuSubList = stuSubService.readStuSubList(studentId);

        // StuSub 리스트 -> StuSubAppDto 리스트로 변환
        List<StuSubAppDto> dtoList = new ArrayList<>();
        int sumGrades = 0;

        for (StuSub ss : stuSubList) {
            if (ss.getSubject() != null) {
                dtoList.add(convertToDto(ss.getSubject()));
                sumGrades += ss.getSubject().getGrades();
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("stuSubList", dtoList); // 변환된 DTO 리스트
        body.put("sumGrades", sumGrades);
        return ResponseEntity.ok(body);
    }

    /**
     * 현재 수강 신청 기간 조회
     */
    @GetMapping("/period")
    public ResponseEntity<?> getSugangPeriod() {
        Map<String, Object> body = new HashMap<>();
        body.put("period", SUGANG_PERIOD);
        body.put("message", getPeriodMessage(SUGANG_PERIOD));
        return ResponseEntity.ok(body);
    }

    /**
     * 수강 신청 기간 메시지 조회 헬퍼 메서드
     */
    private String getPeriodMessage(int period) {
        switch (period) {
            case 0:
                return "현재 예비 수강 신청 기간입니다.";
            case 1:
                return "현재 수강 신청 기간입니다.";
            case 2:
                return "이번 학기 수강 신청 기간이 종료되었습니다.";
            default:
                return "수강 신청 기간 정보를 확인할 수 없습니다.";
        }
    }

    /**
     * 예비 수강 신청 기간 -> 수강 신청 기간으로 변경
     * 경로를 /updatePeriod/1 에서 /period/start로 변경
     */
    @PostMapping("/period/start")
    public ResponseEntity<?> startSugangPeriod(Authentication authentication) {
        // 권한 체크 (staff만 가능)
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        if (!"staff".equals(principal.getUserRole())) {
            throw new CustomRestfullException("권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        if (SUGANG_PERIOD != 0) {
            throw new CustomRestfullException("예비 수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            // 예비 수강 신청 내역을 기반으로 수강 신청 생성
            stuSubService.createStuSubByPreStuSub();

            // 수강 신청 기간으로 변경
            SUGANG_PERIOD = 1;

            Map<String, Object> body = new HashMap<>();
            body.put("period", SUGANG_PERIOD);
            body.put("message", "수강 신청 기간이 시작되었습니다.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new CustomRestfullException("수강 신청 기간 시작에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 수강 신청 기간 -> 수강 신청 종료로 변경
     * 경로를 /updatePeriod/2 에서 /period/end로 변경
     */
    @PostMapping("/period/end")
    public ResponseEntity<?> endSugangPeriod(Authentication authentication) {
        // 권한 체크 (staff만 가능)
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        if (!"staff".equals(principal.getUserRole())) {
            throw new CustomRestfullException("권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        if (SUGANG_PERIOD != 1) {
            throw new CustomRestfullException("수강 신청 기간이 아닙니다.", HttpStatus.BAD_REQUEST);
        }

        try {
            // 수강 신청 기간 종료
            SUGANG_PERIOD = 2;

            Map<String, Object> body = new HashMap<>();
            body.put("period", SUGANG_PERIOD);
            body.put("message", "수강 신청 기간이 종료되었습니다.");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            throw new CustomRestfullException("수강 신청 기간 종료에 실패했습니다: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 수강 신청 기간 초기화 (테스트/관리용)
     * 주의: 프로덕션 환경에서는 제거하거나 추가 보안 검증 필요
     */
    @PostMapping("/period/reset")
    public ResponseEntity<?> resetSugangPeriod(Authentication authentication) {
        // 권한 체크 (staff만 가능)
        PrincipalDto principal = (PrincipalDto) authentication.getPrincipal();
        if (!"staff".equals(principal.getUserRole())) {
            throw new CustomRestfullException("권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        SUGANG_PERIOD = 0;

        Map<String, Object> body = new HashMap<>();
        body.put("period", SUGANG_PERIOD);
        body.put("message", "수강 신청 기간이 초기화되었습니다.");
        return ResponseEntity.ok(body);
    }
}