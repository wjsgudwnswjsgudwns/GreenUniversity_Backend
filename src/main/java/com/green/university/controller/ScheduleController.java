package com.green.university.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

import com.green.university.dto.ScheduleDto;
import com.green.university.dto.ScheduleFormDto;
import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.model.Schedule;
import com.green.university.service.ScheuleService;
import com.green.university.utils.Define;

/**
 * 
 * @author 편용림
 *
 */

/**
 * 학사 일정 관리를 위한 REST 컨트롤러입니다. 일정 조회, 작성, 삭제, 수정 등의 기능을
 * JSON 형식으로 제공합니다.
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

	@Autowired
	private HttpSession session;

	@Autowired
	private ScheuleService scheuleService;

	/**
	 * 학사일정 페이지
	 * 
	 * @param model
	 * @return
	 */
    @GetMapping("")
    public ResponseEntity<List<Schedule>> schedule() {
        List<Schedule> schedule = scheuleService.readSchedule();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> ScheduleList(@RequestParam(defaultValue = "select") String crud) {
        List<Schedule> schedule = scheuleService.readSchedule();
        Map<String, Object> body = new HashMap<>();
        body.put("crud", crud);
        body.put("schedule", schedule);
        return ResponseEntity.ok(body);
    }

	
	//일정 추가
	
    @PostMapping("/write")
    public ResponseEntity<Map<String, String>> ScheduleProc(@RequestBody ScheduleFormDto scheduleFormDto) {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
        if (scheduleFormDto.getStartDay().equals("")){
            throw new CustomRestfullException("날짜를 입력해주세요", HttpStatus.BAD_REQUEST);
        }else if(scheduleFormDto.getEndDay().equals("")){
            throw new CustomRestfullException("날짜를 입력해주세요", HttpStatus.BAD_REQUEST);
        }else if(scheduleFormDto.getInformation().equals("")){
            throw new CustomRestfullException("내용을 입력해주세요", HttpStatus.BAD_REQUEST);
        }else {
            scheuleService.createSchedule(principal.getId(), scheduleFormDto);
        }
        Map<String, String> body = new HashMap<>();
        body.put("message", "일정이 등록되었습니다.");
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @GetMapping("/delete")
    public ResponseEntity<Map<String, String>> deleteSchedule(@RequestParam Integer id) {
        scheuleService.deleteSchedule(id);
        Map<String, String> body = new HashMap<>();
        body.put("message", "일정이 삭제되었습니다.");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/detail")
    public ResponseEntity<Map<String, Object>> detailSchedule(@RequestParam Integer id,
            @RequestParam(defaultValue = "read") String crud) {
        ScheduleDto schedule = scheuleService.readScheduleById(id);
        Map<String, Object> body = new HashMap<>();
        body.put("crud", crud);
        body.put("schedule", schedule);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/update")
    public ResponseEntity<Map<String, String>> updateSchedule(@RequestBody ScheduleFormDto scheduleFormDto) {
        scheuleService.updateSchedule(scheduleFormDto);
        Map<String, String> body = new HashMap<>();
        body.put("message", "일정이 수정되었습니다.");
        return ResponseEntity.ok(body);
    }

}
