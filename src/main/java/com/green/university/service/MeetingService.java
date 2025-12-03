package com.green.university.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.green.university.dto.response.MeetingJoinInfoResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.green.university.dto.CreateMeetingReqDto;

import com.green.university.dto.response.PrincipalDto;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.MeetingJpaRepository;
import com.green.university.repository.MeetingParticipantJpaRepository;
import com.green.university.repository.model.Meeting;
import com.green.university.repository.model.MeetingParticipant;
import com.green.university.repository.model.User;

@Service
public class MeetingService {

    @Autowired
    private MeetingJpaRepository meetingJpaRepository;

    @Autowired
    private MeetingParticipantJpaRepository meetingParticipantJpaRepository;

    @Autowired
    private UserService userService; // 기존 UserService 사용 (user 조회 용)

    private final Random random = new Random();

    /**
     * Janus room 번호 생성 (간단 랜덤, 나중에 중복 체크 필요시 개선 가능).
     */
    private int generateRoomNumber() {
        // 100000 ~ 999999 사이의 6자리 숫자
        return 100000 + random.nextInt(900000);
    }

    /**
     * 즉시 회의 생성.
     */
    @Transactional
    public MeetingSimpleResDto createInstantMeeting(PrincipalDto principal) {
        User host = userService.readUserById(principal.getId());

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setType("INSTANT");

        meeting.setTitle("즉시 회의"); // 나중에 요청에서 제목을 받을 수도 있음
        meeting.setDescription(null);

        LocalDateTime now = LocalDateTime.now();
        meeting.setStartAt(Timestamp.valueOf(now));
        meeting.setEndAt(Timestamp.valueOf(now.plusHours(1)));

        meeting.setStatus("SCHEDULED"); // 입장 시 IN_PROGRESS 등으로 변경 가능
        meeting.setRoomNumber(generateRoomNumber());

        meeting.setCreatedAt(Timestamp.valueOf(now));
        meeting.setUpdatedAt(Timestamp.valueOf(now));

        Meeting saved = meetingJpaRepository.save(meeting);

        // 주최자를 참가자 테이블에도 HOST로 등록
        MeetingParticipant hostParticipant = new MeetingParticipant();
        hostParticipant.setMeeting(saved);
        hostParticipant.setUser(host);
        hostParticipant.setEmail(principal.getEmail()); // PrincipalDto에 이메일 있으면 사용
        hostParticipant.setRole("HOST");
        hostParticipant.setStatus("INVITED");
        meetingParticipantJpaRepository.save(hostParticipant);

        // 응답 DTO 변환
        MeetingSimpleResDto dto = new MeetingSimpleResDto();
        dto.setMeetingId(saved.getId());
        dto.setType(saved.getType());
        dto.setTitle(saved.getTitle());
        dto.setStartAt(saved.getStartAt().toLocalDateTime());
        dto.setEndAt(saved.getEndAt().toLocalDateTime());
        dto.setRoomNumber(saved.getRoomNumber());
        dto.setStatus(saved.getStatus());

        return dto;
    }

    /**
     * 예약 회의 생성.
     */
    @Transactional
    public MeetingSimpleResDto createScheduledMeeting(CreateMeetingReqDto reqDto, PrincipalDto principal) {
        if (reqDto.getStartAt() == null || reqDto.getEndAt() == null) {
            throw new CustomRestfullException("시작/종료 시간이 반드시 필요합니다.", HttpStatus.BAD_REQUEST);
        }
        if (reqDto.getEndAt().isBefore(reqDto.getStartAt())) {
            throw new CustomRestfullException("종료 시간이 시작 시간보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        User host = userService.readUserById(principal.getId());

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setType("SCHEDULED");

        meeting.setTitle(reqDto.getTitle());
        meeting.setDescription(reqDto.getDescription());

        meeting.setStartAt(Timestamp.valueOf(reqDto.getStartAt()));
        meeting.setEndAt(Timestamp.valueOf(reqDto.getEndAt()));

        meeting.setStatus("SCHEDULED");
        meeting.setRoomNumber(generateRoomNumber());

        LocalDateTime now = LocalDateTime.now();
        meeting.setCreatedAt(Timestamp.valueOf(now));
        meeting.setUpdatedAt(Timestamp.valueOf(now));

        Meeting saved = meetingJpaRepository.save(meeting);

        // 주최자를 참가자(HOST)로 등록
        MeetingParticipant hostParticipant = new MeetingParticipant();
        hostParticipant.setMeeting(saved);
        hostParticipant.setUser(host);
        hostParticipant.setEmail(principal.getEmail());
        hostParticipant.setRole("HOST");
        hostParticipant.setStatus("INVITED");
        meetingParticipantJpaRepository.save(hostParticipant);

        MeetingSimpleResDto dto = new MeetingSimpleResDto();
        dto.setMeetingId(saved.getId());
        dto.setType(saved.getType());
        dto.setTitle(saved.getTitle());
        dto.setStartAt(saved.getStartAt().toLocalDateTime());
        dto.setEndAt(saved.getEndAt().toLocalDateTime());
        dto.setRoomNumber(saved.getRoomNumber());
        dto.setStatus(saved.getStatus());

        return dto;
    }

    /**
     * 내가 주최한 회의 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<MeetingSimpleResDto> readMyMeetings(PrincipalDto principal) {
        List<Meeting> meetings = meetingJpaRepository.findByHost_IdOrderByStartAtDesc(principal.getId());

        return meetings.stream().map(m -> {
            MeetingSimpleResDto dto = new MeetingSimpleResDto();
            dto.setMeetingId(m.getId());
            dto.setType(m.getType());
            dto.setTitle(m.getTitle());
            dto.setStartAt(m.getStartAt().toLocalDateTime());
            dto.setEndAt(m.getEndAt().toLocalDateTime());
            dto.setRoomNumber(m.getRoomNumber());
            dto.setStatus(m.getStatus());
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 회의 취소 (주최자만 가능).
     */
    @Transactional
    public void cancelMeeting(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = meetingJpaRepository.findById(meetingId)
                .orElseThrow(() -> new CustomRestfullException("회의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (!meeting.getHost().getId().equals(principal.getId())) {
            throw new CustomRestfullException("회의를 취소할 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if ("CANCELED".equals(meeting.getStatus())) {
            return;
        }

        meeting.setStatus("CANCELED");
        meeting.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        meetingJpaRepository.save(meeting);

        // TODO: 참가자 이메일로 취소 알림 발송 로직
        // meetingParticipantJpaRepository.findByMeeting_Id(meetingId) 사용
    }

    /**
     * 회의 입장 정보 조회 (React + Janus에서 사용).
     * - 시간/상태 체크 후 roomNumber와 표시 이름을 내려준다.
     */
    @Transactional(readOnly = true)
    public MeetingJoinInfoResDto readJoinInfo(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = meetingJpaRepository.findById(meetingId)
                .orElseThrow(() -> new CustomRestfullException("회의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if ("CANCELED".equals(meeting.getStatus())) {
            throw new CustomRestfullException("이미 취소된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        // 시간 체크 (예: 시작 10분 전부터 입장 허용)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = meeting.getStartAt().toLocalDateTime();
        LocalDateTime end = meeting.getEndAt().toLocalDateTime();

        // 필요하면 정책에 맞게 조건 조정
        if (now.isAfter(end)) {
            throw new CustomRestfullException("이미 종료된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        // TODO: 참여자 권한 체크 (호스트 또는 참가자로 등록된 사용자만 허용하고 싶다면)
        // List<MeetingParticipant> participants = meetingParticipantJpaRepository.findByMeeting_Id(meetingId);

        MeetingJoinInfoResDto dto = new MeetingJoinInfoResDto();
        dto.setMeetingId(meeting.getId());
        dto.setTitle(meeting.getTitle());
        dto.setRoomNumber(meeting.getRoomNumber());
        dto.setStartAt(start);
        dto.setEndAt(end);
        dto.setStatus(meeting.getStatus());

        // PrincipalDto에서 이름/역할 가져와서 표기 이름 구성
        dto.setUserRole(principal.getUserRole());
        dto.setDisplayName(principal.getName() + " (" + principal.getUserRole() + ")");

        return dto;
    }
}
