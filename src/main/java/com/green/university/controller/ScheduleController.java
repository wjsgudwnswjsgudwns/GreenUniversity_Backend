package com.green.university.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.green.university.dto.ScheduleDto;
import com.green.university.dto.ScheduleFormDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Schedule;
import com.green.university.service.ScheuleService;

/**
 * 학사일정 REST Controller
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Autowired
    private ScheuleService scheuleService;

    /**
     * 학사일정 전체 조회 (월별 그룹핑)
     * GET /api/schedule
     */
    @GetMapping("")
    public ResponseEntity<List<ScheduleDto>> getScheduleList() {
        List<ScheduleDto> scheduleList = scheuleService.readScheduleDto();
        return ResponseEntity.ok(scheduleList);
    }

    /**
     * 학사일정 관리 페이지용 목록 조회
     * GET /api/schedule/manage
     */
    @GetMapping("/manage")
    public ResponseEntity<List<Schedule>> getScheduleManageList() {
        List<Schedule> scheduleList = scheuleService.readSchedule();
        return ResponseEntity.ok(scheduleList);
    }

    /**
     * 학사일정 상세 조회
     * GET /api/schedule/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScheduleDto> getScheduleDetail(@PathVariable Integer id) {
        ScheduleDto schedule = scheuleService.readScheduleById(id);

        if (schedule == null) {
            throw new CustomRestfullException("학사일정을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }

        return ResponseEntity.ok(schedule);
    }

    /**
     * 학사일정 등록
     * POST /api/schedule
     */
    @PostMapping("")
    public ResponseEntity<Map<String, String>> createSchedule(
            @RequestBody ScheduleFormDto scheduleFormDto,
            Authentication authentication) {

        // 입력 검증
        if (scheduleFormDto.getStartDay() == null || scheduleFormDto.getStartDay().isEmpty()) {
            throw new CustomRestfullException("시작 날짜를 입력해주세요", HttpStatus.BAD_REQUEST);
        }
        if (scheduleFormDto.getEndDay() == null || scheduleFormDto.getEndDay().isEmpty()) {
            throw new CustomRestfullException("종료 날짜를 입력해주세요", HttpStatus.BAD_REQUEST);
        }
        if (scheduleFormDto.getInformation() == null || scheduleFormDto.getInformation().isEmpty()) {
            throw new CustomRestfullException("내용을 입력해주세요", HttpStatus.BAD_REQUEST);
        }

        // 인증된 사용자 ID 가져오기 (JWT에서)
        Object principalObj = authentication.getPrincipal();
        if (!(principalObj instanceof PrincipalDto)) {
            throw new CustomRestfullException("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED);
        }
        PrincipalDto principal = (PrincipalDto) principalObj;
        Integer staffId = principal.getId();

        scheuleService.createSchedule(staffId, scheduleFormDto);

        Map<String, String> response = new HashMap<>();
        response.put("message", "일정이 등록되었습니다.");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 학사일정 수정
     * PUT /api/schedule/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateSchedule(
            @PathVariable Integer id,
            @RequestBody ScheduleFormDto scheduleFormDto) {

        scheduleFormDto.setId(id);
        int result = scheuleService.updateSchedule(scheduleFormDto);

        if (result == 0) {
            throw new CustomRestfullException("학사일정 수정에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "일정이 수정되었습니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 학사일정 삭제
     * DELETE /api/schedule/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSchedule(@PathVariable Integer id) {
        int result = scheuleService.deleteSchedule(id);

        if (result == 0) {
            throw new CustomRestfullException("학사일정 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "일정이 삭제되었습니다.");

        return ResponseEntity.ok(response);
    }
}