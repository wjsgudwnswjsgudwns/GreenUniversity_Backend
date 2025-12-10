package com.green.university.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.green.university.dto.CreateMeetingReqDto;
import com.green.university.dto.response.MeetingJoinInfoResDto;
import com.green.university.dto.response.MeetingPingResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private MeetingService meetingService;


    /**
     * [POST] /api/meetings/instant
     * 즉시 회의 생성.
     * - 로그인된 사용자를 host로 하는 INSTANT 회의를 만든다.
     */
    @PostMapping("/instant")
    public ResponseEntity<MeetingSimpleResDto> createInstantMeeting(@AuthenticationPrincipal PrincipalDto principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        MeetingSimpleResDto dto = meetingService.createInstantMeeting(principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * [POST] /api/meetings
     * 예약 회의 생성.
     * - 요청 바디에 제목, 설명, 시작/종료 시간을 담아 전송.
     */
    @PostMapping("")
    public ResponseEntity<MeetingSimpleResDto> createScheduledMeeting( @AuthenticationPrincipal PrincipalDto principal,
                                                                       @RequestBody CreateMeetingReqDto reqDto) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MeetingSimpleResDto dto = meetingService.createScheduledMeeting(reqDto, principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * [GET] /api/meetings
     * 내가 참여자로 있는 회의 목록 조회.
     */
    @GetMapping("")
    public ResponseEntity<List<MeetingSimpleResDto>> readMyMeetings( @AuthenticationPrincipal PrincipalDto principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<MeetingSimpleResDto> list = meetingService.readMyMeetings(principal);
        return ResponseEntity.ok(list);
    }

    /**
     * [DELETE] /api/meetings/{meetingId}
     * 회의 취소 (주최자만 가능).
     */
    @DeleteMapping("/{meetingId}")
    public ResponseEntity<Map<String, String>> cancelMeeting( @PathVariable Integer meetingId,
                                                              @AuthenticationPrincipal PrincipalDto principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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
    public ResponseEntity<MeetingJoinInfoResDto> readJoinInfo(@PathVariable Integer meetingId,
                                                              @AuthenticationPrincipal PrincipalDto principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        MeetingJoinInfoResDto dto = meetingService.readJoinInfo(meetingId, principal);
        return ResponseEntity.ok(dto);
    }

    /**
     * [POST] /api/meetings/{meetingId}/participants/join
     * 회의 참가 API (참가자 목록에 나 자신 기록)
     */
    @PostMapping("/{meetingId}/participants/join")
    public ResponseEntity<Map<String, String>> joinMeeting(
            @PathVariable Integer meetingId,
            @AuthenticationPrincipal PrincipalDto principal
    ) {
        if (principal == null) {
            // 로그인 안됐을때
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String sessionKey = meetingService.joinMeeting(meetingId, principal);



        Map<String, String> body = new HashMap<>();
        body.put("message", "회의에 참가했습니다.");
        body.put("sessionKey", sessionKey);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/{meetingId}/participants/leave")
    public ResponseEntity<Map<String, String>> leaveMeeting(
            @PathVariable Integer meetingId,
            @AuthenticationPrincipal PrincipalDto principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        meetingService.leaveMeeting(meetingId, principal);

        Map<String, String> body = new HashMap<>();
        body.put("message", "회의에서 나갔습니다.");
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{meetingId}/participants/ping")
    public ResponseEntity<MeetingPingResDto> ping(
            @PathVariable Integer meetingId,
            @AuthenticationPrincipal PrincipalDto principal,
            @RequestBody Map<String, String> body
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String clientSessionKey = body.get("sessionKey"); // 프론트에서 보낸 세션 키

        MeetingPingResDto dto =
                meetingService.ping(meetingId, principal, clientSessionKey);

        return ResponseEntity.ok(dto);
    }

}
