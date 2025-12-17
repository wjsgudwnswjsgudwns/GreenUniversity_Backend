package com.green.university.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.green.university.dto.ChatMessageDto;
import com.green.university.dto.MediaStateSignalMessageDto;
import com.green.university.dto.PresenceEventDto;
import com.green.university.dto.response.MeetingJoinInfoResDto;
import com.green.university.dto.response.MeetingPingResDto;
import com.green.university.dto.response.MeetingSimpleResDto;
import com.green.university.enums.MeetingStatus;
import com.green.university.presence.MediaStateStore;
import com.green.university.presence.PresenceStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

@Slf4j
@Service
public class MeetingService {

    @Autowired
    private MeetingJpaRepository meetingJpaRepository;

    @Autowired
    private MeetingParticipantJpaRepository meetingParticipantJpaRepository;

    @Autowired
    private UserService userService; // 기존 UserService 사용 (user 조회 용)

    @Autowired
    private MeetingChatService meetingChatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private PresenceStore presenceStore;
    @Autowired
    private MediaStateStore mediaStateStore;
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

    private void validateJoinWindow(Meeting meeting, LocalDateTime now) {
        // INSTANT 회의는 시간 제한 없이 바로 입장 가능
        if ("INSTANT".equals(meeting.getType())) {
            return;
        }

        LocalDateTime start = meeting.getStartAt().toLocalDateTime();
        LocalDateTime end = meeting.getEndAt().toLocalDateTime();

        // 시작 10분 전부터 입장 가능
        if (now.isBefore(start.minusMinutes(1000))) {
            throw new CustomRestfullException(
                    "회의 시작 10분 전부터 입장이 가능합니다.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // (선택) 종료 후 10분까지는 입장 허용, 그 이후는 막기
        if (now.isAfter(end.plusMinutes(10))) {
            throw new CustomRestfullException(
                    "회의 입장 가능 시간이 지났습니다.",
                    HttpStatus.BAD_REQUEST
            );
        }
    }


    @Transactional
    public MeetingParticipant addGuestParticipant(
            Integer meetingId,
            String email,
            Integer userId
    ) {
        Meeting meeting = meetingJpaRepository.findById(meetingId)
                .orElseThrow(() ->
                        new CustomRestfullException("회의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        User user = userService.readUserById(userId);

        // 이미 있으면 갱신, 없으면 새로 생성 (upsert)
        MeetingParticipant p = meetingParticipantJpaRepository
                .findByMeeting_IdAndUser_Id(meetingId, userId)
                .orElseGet(MeetingParticipant::new);

        p.setMeeting(meeting);
        p.setUser(user);
        p.setEmail(email);
        p.setRole("PARTICIPANT");  // 🔥 게스트는 항상 PARTICIPANT
        p.setStatus("INVITED");

        // 초대/등록 시점이므로 세션 정보 초기화
        p.setSessionKey(null);
        p.setJoinedAt(null);
        p.setLeftAt(null);
        p.setLastActiveAt(null);

        return meetingParticipantJpaRepository.save(p);
    }

    /**
     * 공통: 현재 시간 기준으로 회의 상태 갱신
     * <p>
     * - CANCELED / FINISHED 는 건들지 않음
     * - endAt + 10분이 지나면 FINISHED
     * - lastEmptyAt + 30분이 지나면 FINISHED
     *
     * @return true  : 이 호출에서 FINISHED 로 변경됨
     * false : 상태 변화 없음
     */
    private boolean refreshMeetingStatus(Meeting meeting) {
        LocalDateTime now = LocalDateTime.now();

        if (meeting.getEndAt() == null) {
            // endAt 없는 회의는 자동 종료 로직을 안 탄다고 가정
            return false;
        }

        LocalDateTime end = meeting.getEndAt().toLocalDateTime();
        LocalDateTime lastEmpty = meeting.getLastEmptyAt() != null
                ? meeting.getLastEmptyAt().toLocalDateTime()
                : null;

        // 이미 취소/종료된 회의면 더 이상 처리하지 않음
        if (MeetingStatus.CANCELED.equals(meeting.getStatus())
                || MeetingStatus.FINISHED.equals(meeting.getStatus())) {
            return false;
        }

        boolean shouldFinish = false;

        // 종료시간 + 10분이 지나면 자동 종료
        if (now.isAfter(end.plusMinutes(10))) {
            shouldFinish = true;
        }

        // 아무도 없는 상태(lastEmptyAt)로 30분 지났으면 자동 종료
        if (lastEmpty != null && now.isAfter(lastEmpty.plusMinutes(30))) {
            shouldFinish = true;
        }

        if (shouldFinish) {
            meeting.setStatus(MeetingStatus.FINISHED);
            meeting.setUpdatedAt(Timestamp.valueOf(now));
            meetingJpaRepository.save(meeting);
            return true;
        }

        return false;
    }

    /**
     * 참가자 upsert + 세션 키 발급 + 회의 상태 IN_PROGRESS 로 전환
     * - INSTANT / SCHEDULED 공통 사용
     */
    private String upsertParticipantAndIssueSessionKey(Meeting meeting, PrincipalDto principal) {
        LocalDateTime now = LocalDateTime.now();

        // 기존 참가자 있으면 재사용, 없으면 새로 생성
        MeetingParticipant p = meetingParticipantJpaRepository
                .findByMeeting_IdAndUser_Id(meeting.getId(), principal.getId())
                .orElseGet(MeetingParticipant::new);

        boolean wasJoined = "JOINED".equals(p.getStatus()) && p.getLeftAt() == null;

        String newSessionKey = UUID.randomUUID().toString();

        p.setMeeting(meeting);
        p.setUser(userService.readUserById(principal.getId()));
        p.setEmail(principal.getEmail());

        // displayName 보정 (User 엔티티에 이름 없을 수 있으니)
        String dn = principal.getName();
        if (dn == null || dn.isBlank()) dn = principal.getEmail();
        if (dn == null || dn.isBlank()) dn = "참가자";
        p.setDisplayName(dn);

        // 이미 HOST로 등록된 경우(role이 이미 있을 경우) 그대로 유지
        if (p.getRole() == null) {
            p.setRole("PARTICIPANT");
        }

        p.setStatus("JOINED");
        if (!wasJoined) {
            p.setJoinedAt(Timestamp.valueOf(now));
        }
        p.setLeftAt(null);

        p.setSessionKey(newSessionKey);
        p.setLastActiveAt(Timestamp.valueOf(now));

        meetingParticipantJpaRepository.save(p);

        // 회의 상태를 진행 중으로
        if (!MeetingStatus.IN_PROGRESS.equals(meeting.getStatus())) {
            meeting.setStatus(MeetingStatus.IN_PROGRESS);
        }
        meeting.setLastEmptyAt(null); // 방이 비어있지 않음
        meeting.setUpdatedAt(Timestamp.valueOf(now));
        meetingJpaRepository.save(meeting);

        //  여기서 "새로 JOIN"일 때만 SYSTEM 쏘기
        if (!wasJoined) {
            ChatMessageDto systemMsg = meetingChatService.saveSystemMessage(
                    meeting.getId(),
                    dn + " 님이 회의에 입장했습니다."
            );
            messagingTemplate.convertAndSend(
                    String.format("/sub/meetings/%d/chat", meeting.getId()),
                    systemMsg
            );
            broadcastPresenceJoin(meeting.getId(), p);
        }
        return newSessionKey;
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

        // 주최자를 참가자 테이블에도 HOST로 등록 (초기 상태는 INVITED)
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
        if (reqDto.getEndAt().before(reqDto.getStartAt())) {
            throw new CustomRestfullException("종료 시간이 시작 시간보다 빠를 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        User host = userService.readUserById(principal.getId());

        Meeting meeting = new Meeting();
        meeting.setHost(host);
        meeting.setType("SCHEDULED");

        meeting.setTitle(reqDto.getTitle());
        meeting.setDescription(reqDto.getDescription());

        meeting.setStartAt(reqDto.getStartAt());
        meeting.setEndAt(reqDto.getEndAt());

        meeting.setStatus(MeetingStatus.SCHEDULED);
        meeting.setRoomNumber(generateRoomNumber());

        LocalDateTime now = LocalDateTime.now();
        meeting.setCreatedAt(Timestamp.valueOf(now));
        meeting.setUpdatedAt(Timestamp.valueOf(now));

        Meeting saved = meetingJpaRepository.save(meeting);

        // 주최자를 참가자(HOST)로 등록 (초기 상태는 INVITED)
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
        List<MeetingParticipant> participants =
                meetingParticipantJpaRepository
                        .findByUser_IdOrderByMeeting_StartAtDesc(principal.getId());

        return participants.stream()
                .map(p -> {
                    Meeting m = p.getMeeting();  // 핵심: Participant → Meeting 꺼내기

                    MeetingSimpleResDto dto = new MeetingSimpleResDto();
                    dto.setMeetingId(m.getId());
                    dto.setType(m.getType());
                    dto.setTitle(m.getTitle());
                    dto.setStartAt(m.getStartAt().toLocalDateTime());
                    dto.setEndAt(m.getEndAt().toLocalDateTime());
                    dto.setRoomNumber(m.getRoomNumber());
                    dto.setStatus(m.getStatus().name());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 회의 취소 (주최자만 가능).
     */
    @Transactional
    public void cancelMeeting(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = findById(meetingId);

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
     * - INSTANT 회의면 이 시점에서 자동 참가 + 세션 키 발급
     */
    @Transactional
    public MeetingJoinInfoResDto readJoinInfo(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = findById(meetingId);

        // 1) 공통 상태 갱신
        boolean finished = refreshMeetingStatus(meeting);

        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            throw new CustomRestfullException("이미 취소된 회의입니다.", HttpStatus.BAD_REQUEST);
        }
        if (finished || MeetingStatus.FINISHED.equals(meeting.getStatus())) {
            throw new CustomRestfullException("이미 종료된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();

        validateJoinWindow(meeting, now);

        LocalDateTime start = meeting.getStartAt().toLocalDateTime();
        LocalDateTime end = meeting.getEndAt().toLocalDateTime();

        MeetingJoinInfoResDto dto = new MeetingJoinInfoResDto();
        dto.setMeetingId(meeting.getId());
        dto.setTitle(meeting.getTitle());
        dto.setRoomNumber(meeting.getRoomNumber());
        dto.setStartAt(start);
        dto.setEndAt(end);
        dto.setStatus(meeting.getStatus().name());
        dto.setUserId(principal.getId());
        dto.setUserRole(principal.getUserRole());
        dto.setDisplayName(principal.getName());


        Integer hostUserId = meeting.getHost().getId();   // ← 실제 필드명에 맞게 수정
        dto.setHostUserId(hostUserId);

        boolean isHost = hostUserId != null && hostUserId.equals(principal.getId());
        dto.setIsHost(isHost);
        dto.setSessionKey(null);

        return dto;
    }

    /**
     * 예약 회의 등의 명시적 "참가하기" 버튼용
     */
    @Transactional
    public String joinMeeting(Integer meetingId, PrincipalDto principal) {
        Meeting meeting = findById(meetingId);
        boolean finished = refreshMeetingStatus(meeting);

        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            throw new CustomRestfullException("취소된 회의입니다.", HttpStatus.BAD_REQUEST);
        }
        if (finished || MeetingStatus.FINISHED.equals(meeting.getStatus())) {
            throw new CustomRestfullException("이미 종료된 회의입니다.", HttpStatus.BAD_REQUEST);
        }

        LocalDateTime now = LocalDateTime.now();
        validateJoinWindow(meeting, now);

        Integer userId = principal.getId();
        String dn = principal.getName();
        if (dn == null || dn.isBlank()) dn = principal.getEmail();
        if (dn == null || dn.isBlank()) dn = "참가자";

        // HOST 여부는 meeting 기준으로 판단
        String role = meeting.getHost() != null && meeting.getHost().getId().equals(userId)
                ? "HOST"
                : "PARTICIPANT";

        // 메모리 기반 세션키 발급/교체
        String sessionKey = presenceStore.joinOrReplace(meetingId, userId, dn, role);

        // Presence JOIN 브로드캐스트
        PresenceEventDto payload = PresenceEventDto.builder()
                .type("JOIN")
                .meetingId(meetingId)
                .userId(userId)
                .displayName(dn)
                .role(role)
                .joined(true)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );

        return sessionKey;
    }


    @Transactional
    public void leaveMeeting(Integer meetingId, PrincipalDto principal) {
        Integer userId = principal.getId();
        presenceStore.leave(meetingId, userId);
        mediaStateStore.remove(meetingId, userId); // 미디어 상태도 삭제

        PresenceEventDto payload = PresenceEventDto.builder()
                .type("LEAVE")
                .meetingId(meetingId)
                .userId(userId)
                .joined(false)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );
    }


    @Transactional
    public MeetingPingResDto ping(Integer meetingId,
                                  PrincipalDto principal,
                                  String clientSessionKey) {

        LocalDateTime now = LocalDateTime.now();
        Meeting meeting = findById(meetingId);

        boolean finished = refreshMeetingStatus(meeting);

        if (MeetingStatus.CANCELED.equals(meeting.getStatus())) {
            return MeetingPingResDto.inactive("MEETING_CANCELED");
        }
        if (finished || MeetingStatus.FINISHED.equals(meeting.getStatus())) {
            return MeetingPingResDto.inactive("MEETING_FINISHED");
        }

        Integer userId = principal.getId();

        // 메모리 기반 유효성 체크
        com.green.university.presence.PresenceStore.State s = presenceStore.get(meetingId, userId);
        if (s == null) {
            return MeetingPingResDto.inactive("NOT_JOINED");
        }

        boolean ok = presenceStore.heartbeat(meetingId, userId, clientSessionKey);
        if (!ok) {
            return MeetingPingResDto.inactive("SESSION_REPLACED");
        }

        MeetingPingResDto dto = new MeetingPingResDto();
        dto.setActive(true);
        dto.setReason(null);
        return dto;
    }


    @Transactional(readOnly = true)
    public void broadcastPresenceSync(Integer meetingId) {
        List<PresenceStore.State> list = presenceStore.list(meetingId);

        List<PresenceEventDto.ParticipantDto> participants = list.stream()
                .map(s -> PresenceEventDto.ParticipantDto.builder()
                        .userId(s.getUserId())
                        .displayName(s.getDisplayName())
                        .role(s.getRole())
                        .joined(true)
                        .build())
                .toList();

        PresenceEventDto payload = PresenceEventDto.builder()
                .type("SYNC")
                .meetingId(meetingId)
                .participants(participants)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );

        log.debug("[Presence] SYNC(sent from memory) meetingId={}, size={}", meetingId, participants.size());

        // presence SYNC 후 각 참가자의 미디어 상태도 방송하여 새로 입장한 사용자가 즉시 상태를 알 수 있게 함
        broadcastMediaStateSync(meetingId);
    }

    @Transactional(readOnly = true)
    public void broadcastMediaStateSync(Integer meetingId) {
        List<MediaStateStore.State> list = mediaStateStore.list(meetingId);
        if (list == null || list.isEmpty()) return;

        for (MediaStateStore.State s : list) {
            if (s == null) continue;
            MediaStateSignalMessageDto msg = new MediaStateSignalMessageDto();
            msg.setMeetingId(meetingId);
            msg.setUserId(s.getUserId());
            msg.setDisplay(s.getDisplay());
            msg.setAudio(s.getAudio());
            msg.setVideo(s.getVideo());
            msg.setVideoDeviceLost(s.getVideoDeviceLost());
            msg.setType("MEDIA_STATE");

            messagingTemplate.convertAndSend(
                    String.format("/sub/meetings/%d/signals", meetingId),
                    msg
            );
        }
    }

    private void broadcastPresenceJoin(Integer meetingId, MeetingParticipant p) {
        PresenceEventDto payload = PresenceEventDto.builder()
                .type("JOIN")
                .meetingId(meetingId)
                .userId(p.getUser().getId())
                .displayName(
                        (p.getDisplayName() != null && !p.getDisplayName().isBlank())
                                ? p.getDisplayName()
                                : (p.getEmail() != null ? p.getEmail() : "참가자")
                )
                .role(p.getRole() != null ? p.getRole() : "PARTICIPANT")
                .joined(true)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );
    }

    private void broadcastPresenceLeave(Integer meetingId, Integer userId) {
        PresenceEventDto payload = PresenceEventDto.builder()
                .type("LEAVE")
                .meetingId(meetingId)
                .userId(userId)
                .joined(false)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );
    }
    @Transactional
    public void leaveKeepalive(Integer meetingId, String sessionKey) {
        Integer userId = presenceStore.findUserIdBySessionKey(meetingId, sessionKey);
        if (userId == null) return;
        presenceStore.leave(meetingId, userId);
        mediaStateStore.remove(meetingId, userId); // 미디어 상태도 삭제

        PresenceEventDto payload = PresenceEventDto.builder()
                .type("LEAVE")
                .meetingId(meetingId)
                .userId(userId)
                .joined(false)
                .build();

        messagingTemplate.convertAndSend("/sub/meetings/" + meetingId + "/presence", payload);
    }

}
