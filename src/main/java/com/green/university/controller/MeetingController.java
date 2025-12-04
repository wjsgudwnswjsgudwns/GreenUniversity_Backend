package com.green.university.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.green.university.dto.CreateMeetingReqDto;
import com.green.university.dto.response.MeetingJoinInfoResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.service.MeetingService;
import com.green.university.utils.Define;

/**
 * 화상 회의(예약/즉시 회의) 관련 REST API 컨트롤러.
 */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    @Autowired
    private HttpSession session;

    @Autowired
    private MeetingService meetingService;

    /**
     * 세션에서 로그인 사용자 정보(PrincipalDto)를 가져온다.
     */
    private PrincipalDto getPrincipal() {
        PrincipalDto principal = (PrincipalDto) session.getAttribute(Define.PRINCIPAL);
//        if (principal == null) {
//            throw new CustomRestfullException("로그인이 필요합니다.", HttpStatus.UNAUTHORIZED);
//        }

        if (principal == null) {
            // ★ 임시 테스트용 가짜 Principal
            principal = new PrincipalDto();
           // principal.setId(1);              // 실제 존재하는 유저 id로
            principal.setUserRole("student");
            principal.setName("테스트유저");
            principal.setEmail("test@example.com");
            principal.setId(2023000001);
        }
        return principal;
    }

    /**
     * [POST] /api/meetings/instant
     * 즉시 회의 생성.
     * - 로그인된 사용자를 host로 하는 INSTANT 회의를 만든다.
     */
    @PostMapping("/instant")
    public ResponseEntity<MeetingSimpleResDto> createInstantMeeting() {
        PrincipalDto principal = getPrincipal();
        MeetingSimpleResDto dto = meetingService.createInstantMeeting(principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * [POST] /api/meetings
     * 예약 회의 생성.
     * - 요청 바디에 제목, 설명, 시작/종료 시간을 담아 전송.
     */
    @PostMapping("")
    public ResponseEntity<MeetingSimpleResDto> createScheduledMeeting(@RequestBody CreateMeetingReqDto reqDto) {
        PrincipalDto principal = getPrincipal();
        MeetingSimpleResDto dto = meetingService.createScheduledMeeting(reqDto, principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * [GET] /api/meetings
     * 내가 주최한 회의 목록 조회.
     */
    @GetMapping("")
    public ResponseEntity<List<MeetingSimpleResDto>> readMyMeetings() {
        PrincipalDto principal = getPrincipal();
        List<MeetingSimpleResDto> list = meetingService.readMyMeetings(principal);
        return ResponseEntity.ok(list);
    }

    /**
     * [DELETE] /api/meetings/{meetingId}
     * 회의 취소 (주최자만 가능).
     */
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Map<String, String>> cancelMeeting(@PathVariable Integer meetingId) {
        PrincipalDto principal = getPrincipal();
        meetingService.cancelMeeting(meetingId, principal);

        Map<String, String> body = new HashMap<>();
        body.put("message", "회의가 취소되었습니다.");
        return ResponseEntity.ok(body);
    }

    /**
     * [GET] /api/meetings/{meetingId}/join-info
     * 회의 입장 정보 조회.
     * - React에서 회의방(/meeting/:id) 들어가기 직전에 호출.
     * - roomNumber, displayName, 시간/상태 등을 반환.
     */
    @GetMapping("/{meetingId}/join-info")
    public ResponseEntity<MeetingJoinInfoResDto> readJoinInfo(@PathVariable Integer meetingId) {
        PrincipalDto principal = getPrincipal();
        MeetingJoinInfoResDto dto = meetingService.readJoinInfo(meetingId, principal);
        return ResponseEntity.ok(dto);
    }
}
