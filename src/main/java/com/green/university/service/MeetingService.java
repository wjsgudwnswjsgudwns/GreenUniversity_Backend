package com.green.university.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.green.university.dto.response.MeetingJoinInfoResDto;
import com.green.university.dto.response.MeetingPingResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import com.green.university.enums.MeetingStatus;
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
    private Meeting findById(Integer meetingId) {
        return meetingJpaRepository.findById(meetingId)
                .orElseThrow(() -> new CustomRestfullException("회의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
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

        meeting.setStatus(MeetingStatus.IN_PROGRESS); // 즉시 회의는 바로 진행 중으로
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
        dto.setStatus(saved.getStatus().name());

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

        meeting.setStatus(MeetingStatus.SCHEDULED);
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
        dto.setStatus(saved.getStatus().name());

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
            dto.setStatus(m.getStatus().name());
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
        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            return;
        }

        meeting.setStatus(MeetingStatus.CANCELED);
        meeting.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        meetingJpaRepository.save(meeting);

        // TODO: 참가자 이메일로 취소 알림 발송 로직
        // meetingParticipantJpaRepository.findByMeeting_Id(meetingId) 사용
    }

    /**
     * 회의 입장 정보 조회 (React + Janus에서 사용).
     * - 시간/상태 체크 후 roomNumber와 표시 이름을 내려준다.
     */
    @Transactional
    public MeetingJoinInfoResDto readJoinInfo(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = meetingJpaRepository.findById(meetingId)
                .orElseThrow(() -> new CustomRestfullException("회의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = meeting.getStartAt().toLocalDateTime();
        LocalDateTime end = meeting.getEndAt().toLocalDateTime();
        LocalDateTime lastEmpty = meeting.getLastEmptyAt() != null
                ? meeting.getLastEmptyAt().toLocalDateTime()
                : null;

        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            throw new CustomRestfullException("이미 취소된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        if (now.isAfter(end)) {
            meeting.setStatus(MeetingStatus.FINISHED);
            meeting.setUpdatedAt(Timestamp.valueOf(now));
            meetingJpaRepository.save(meeting);
            throw new CustomRestfullException("이미 종료된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        if (lastEmpty != null && now.isAfter(lastEmpty.plusMinutes(30))) {
            meeting.setStatus(MeetingStatus.FINISHED);
            meeting.setUpdatedAt(Timestamp.valueOf(now));
            meetingJpaRepository.save(meeting);
            throw new CustomRestfullException("모든 참가자가 나간 뒤 일정 시간이 지나 회의가 종료되었습니다.", HttpStatus.BAD_REQUEST);
        }



        // TODO: 참여자 권한 체크 (호스트 또는 참가자로 등록된 사용자만 허용하고 싶다면)
        // List<MeetingParticipant> participants = meetingParticipantJpaRepository.findByMeeting_Id(meetingId);

        MeetingJoinInfoResDto dto = new MeetingJoinInfoResDto();
        dto.setMeetingId(meeting.getId());
        dto.setTitle(meeting.getTitle());
        dto.setRoomNumber(meeting.getRoomNumber());
        dto.setStartAt(start);
        dto.setEndAt(end);
        dto.setStatus(meeting.getStatus().name());
        dto.setUserId(principal.getId());
        // PrincipalDto에서 이름/역할 가져와서 표기 이름 구성
        dto.setUserRole(principal.getUserRole());
        dto.setDisplayName(principal.getName() + " (" + principal.getUserRole() + ")");

        return dto;
    }

    @Transactional
    public String joinMeeting(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = findById(meetingId);

        if (meeting.getStatus() == MeetingStatus.CANCELED) {
            throw new CustomRestfullException("취소된 회의입니다.", HttpStatus.BAD_REQUEST);
        }
        if (meeting.getStatus() == MeetingStatus.FINISHED) {
            throw new CustomRestfullException("이미 종료된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endAt = meeting.getEndAt().toLocalDateTime();
        if (now.isAfter(endAt.plusMinutes(10))) { // 예: 종료 시간 + 10분까지는 허용
            throw new CustomRestfullException("회의 시간이 종료되었습니다.", HttpStatus.BAD_REQUEST);
        }

        if (meeting.getLastEmptyAt() != null) {
            LocalDateTime lastEmptyAt = meeting.getLastEmptyAt().toLocalDateTime();
            if (now.isAfter(lastEmptyAt.plusMinutes(30))) { // 30분 동안 아무도 없었으면 회의 끝난 것으로 간주
                meeting.setStatus(MeetingStatus.FINISHED);
                meeting.setUpdatedAt(Timestamp.valueOf(now));
                meetingJpaRepository.save(meeting);
                throw new CustomRestfullException("참가자가 없어 회의가 자동 종료되었습니다.", HttpStatus.BAD_REQUEST);
            }
        }
        // 2) participant 찾기 (없으면 새로 생성)
        MeetingParticipant p = meetingParticipantJpaRepository
                .findByMeeting_IdAndUser_Id(meetingId, principal.getId())
                .orElse(new MeetingParticipant());
        // 새 세션 키 생성
        String newSessionKey = UUID.randomUUID().toString();

        p.setMeeting(meeting);
        p.setUser(userService.readUserById(principal.getId()));
        p.setEmail(principal.getEmail());
        p.setRole(p.getRole() == null ? "PARTICIPANT" : p.getRole());
        p.setStatus("JOINED");
        p.setJoinedAt(Timestamp.valueOf(now));
        p.setLeftAt(null);

        p.setSessionKey(newSessionKey);
        p.setLastActiveAt(Timestamp.valueOf(now));

        meetingParticipantJpaRepository.save(p);

        //  회의 상태 업데이트
        if (!MeetingStatus.IN_PROGRESS.equals(meeting.getStatus())) {
            meeting.setStatus(MeetingStatus.IN_PROGRESS);
        }
        meeting.setLastEmptyAt(null); // 이제 방은 비어 있지 않음
        meeting.setUpdatedAt(Timestamp.valueOf(now));
        meetingJpaRepository.save(meeting);

        return newSessionKey;
    }

    @Transactional
    public void leaveMeeting(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = findById(meetingId);

        MeetingParticipant p =
                meetingParticipantJpaRepository
                        .findByMeeting_IdAndUser_Id(meetingId, principal.getId())
                        .orElse(null);
        if (p == null) {
            return;
        }

        p.setStatus("LEFT");
        p.setLeftAt(Timestamp.valueOf(LocalDateTime.now()));
        meetingParticipantJpaRepository.save(p);

        // 이 회의에 JOINED 상태의 사람이 아직 있는지 확인
        boolean hasJoined =
                meetingParticipantJpaRepository.existsByMeeting_IdAndStatus(meetingId, "JOINED");

        if (!hasJoined) {
            // 아무도 없으면 방이 '비어 있음'
            meeting.setLastEmptyAt(Timestamp.valueOf(LocalDateTime.now()));
            meeting.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
            meetingJpaRepository.save(meeting);
        }
    }

    @Transactional
    public MeetingPingResDto ping(Integer meetingId,
                                  PrincipalDto principal,
                                  String clientSessionKey) {

        LocalDateTime now = LocalDateTime.now();
        Meeting meeting = findById(meetingId);

        // 1) 회의 상태/시간 체크
        LocalDateTime endAt = meeting.getEndAt().toLocalDateTime();
        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            return MeetingPingResDto.inactive("MEETING_CANCELED");
        }

        if (now.isAfter(endAt.plusMinutes(10))) {
            meeting.setStatus(MeetingStatus.FINISHED);
            meeting.setUpdatedAt(Timestamp.valueOf(now));
            meetingJpaRepository.save(meeting);

            return MeetingPingResDto.inactive("MEETING_FINISHED");
        }

        // 2) 내 참가 정보 조회
        MeetingParticipant p = meetingParticipantJpaRepository
                .findByMeeting_IdAndUser_Id(meetingId, principal.getId())
                .orElse(null);

        if (p == null) {
            return MeetingPingResDto.inactive("NOT_JOINED");
        }

        // 3) 세션키 비교 (다른 브라우저에서 재접속한 경우)
        String serverSessionKey = p.getSessionKey();
        if (serverSessionKey != null
                && clientSessionKey != null
                && !serverSessionKey.equals(clientSessionKey)) {

            // 이 브라우저는 더 이상 유효하지 않은 옛날 세션
            return MeetingPingResDto.inactive("SESSION_REPLACED");
        }

        // 4) 여기까지 왔으면 "유효한 현재 세션" → heartbeat 갱신
        p.setLastActiveAt(Timestamp.valueOf(now));
        meetingParticipantJpaRepository.save(p);

        MeetingPingResDto dto = new MeetingPingResDto();
        dto.setActive(true);
        dto.setReason(null);
        return dto;
    }

}
