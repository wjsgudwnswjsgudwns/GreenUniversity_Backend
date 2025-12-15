package com.green.university.service;

import com.green.university.dto.ChatMessageDto;
import com.green.university.enums.ChatMessageType;
import com.green.university.handler.exception.CustomRestfullException;
import com.green.university.repository.MeetingChatJpaRepository;
import com.green.university.repository.MeetingJpaRepository;
import com.green.university.repository.UserJpaRepository;
import com.green.university.repository.model.Meeting;
import com.green.university.repository.model.MeetingChat;
import com.green.university.repository.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeetingChatService {

    private final MeetingChatJpaRepository chatRepo;
    private final MeetingJpaRepository meetingRepo;
    private final UserJpaRepository userRepo;

    // 기본 / 최소 / 최대 page size
    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MIN_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 200;

    /**
     * size 파라미터 정규화:
     * - null 이면 DEFAULT
     * - 너무 작으면 MIN
     * - 너무 크면 MAX
     */
    private int normalizeSize(Integer sizeParam) {
        int size = (sizeParam != null) ? sizeParam : DEFAULT_PAGE_SIZE;
        if (size < MIN_PAGE_SIZE) size = MIN_PAGE_SIZE;
        if (size > MAX_PAGE_SIZE) size = MAX_PAGE_SIZE;
        return size;
    }

    /**
     * WebSocket으로 들어온 채팅 저장.
     * - meetingId 로 회의 검증
     * - userId 로 유저 검증
     * - DB 저장 후 ChatMessageDto 로 반환
     */
    @Transactional
    public ChatMessageDto saveChatMessage(
            Integer meetingId,
            Integer senderUserId,
            String senderDisplayName,
            String messageText,
            String messageType
    ) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() -> new CustomRestfullException("존재하지 않는 회의입니다.", HttpStatus.NOT_FOUND));

        MeetingChat chat = new MeetingChat();
        chat.setMeeting(meeting);
        chat.setMessage(messageText);
        chat.setSentAt(Timestamp.valueOf(LocalDateTime.now()));

        // 1) type 결정 (기본 CHAT)
        ChatMessageType type;
        try {
            type = (messageType == null || messageType.isBlank())
                    ? ChatMessageType.CHAT
                    : ChatMessageType.valueOf(messageType.trim().toUpperCase());
        } catch (Exception e) {
            type = ChatMessageType.CHAT;
        }
        chat.setType(type);

        // 2) SYSTEM이면 sender null 허용
        if (type == ChatMessageType.SYSTEM) {
            chat.setSender(null);
            chat.setSenderName(null);
        } else {
            if (senderUserId == null) {
                throw new CustomRestfullException("userId가 없습니다.", HttpStatus.BAD_REQUEST);
            }
            User sender = userRepo.findById(senderUserId)
                    .orElseThrow(() -> new CustomRestfullException("존재하지 않는 사용자입니다.", HttpStatus.NOT_FOUND));
            chat.setSender(sender);
            chat.setSenderName(
                    (senderDisplayName == null || senderDisplayName.isBlank()) ? "참가자" : senderDisplayName
            );
        }

        MeetingChat saved = chatRepo.save(chat);
        return toDto(saved);
    }


    @Transactional
    public ChatMessageDto saveSystemMessage(Integer meetingId, String messageText) {
        Meeting meeting = meetingRepo.findById(meetingId)
                .orElseThrow(() ->
                        new CustomRestfullException("존재하지 않는 회의입니다.", HttpStatus.NOT_FOUND)
                );

        MeetingChat chat = new MeetingChat();
        chat.setMeeting(meeting);
        chat.setSender(null);          // SYSTEM
        chat.setSenderName(null);      // 표시 안 함
        chat.setMessage(messageText);
        chat.setType(ChatMessageType.SYSTEM);

        MeetingChat saved = chatRepo.save(chat);
        return toDto(saved);
    }

    /**
     * 최근 메시지들 조회 (초기 입장 시)
     * - size: 기본값 30, 파라미터로 조절 가능
     * - 오래된 → 최신 순(ASC)으로 반환
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getRecentMessages(Integer meetingId, Integer sizeParam) {
        int size = normalizeSize(sizeParam);

        Pageable pageable = PageRequest.of(
                0,
                size,
                Sort.by(Sort.Direction.DESC, "sentAt")  // 최신부터 가져와서
        );

        List<MeetingChat> list = chatRepo.findByMeeting_Id(meetingId, pageable);

        // DESC 로 가져온 걸 ASC 로 뒤집기
        Collections.reverse(list);

        return list.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 특정 메시지 이후(afterId) 메시지들 조회
     * - F5 / 재입장 시 "중간에 쌓인 구간" 채우기용
     * - id > afterId 인 메시지들을 오래된 → 최신 순(ASC)으로 반환
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesAfter(Integer meetingId, Integer afterId, Integer sizeParam) {
        int size = normalizeSize(sizeParam);

        Pageable pageable = PageRequest.of(
                0,
                size,
                Sort.by(Sort.Direction.ASC, "id") // id 오름차순
        );

        List<MeetingChat> list =
                chatRepo.findByMeeting_IdAndIdGreaterThan(meetingId, afterId, pageable);

        return list.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 특정 메시지 이전(beforeId) 메시지들 조회
     * - 위로 스크롤할 때 과거 기록 로딩용
     * - id < beforeId 인 메시지들을 오래된 → 최신 순(ASC)으로 반환
     */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessagesBefore(Integer meetingId, Integer beforeId, Integer sizeParam) {
        int size = normalizeSize(sizeParam);

        Pageable pageable = PageRequest.of(
                0,
                size,
                Sort.by(Sort.Direction.DESC, "id") // 최신 → 과거로 가져온 다음
        );

        List<MeetingChat> list =
                chatRepo.findByMeeting_IdAndIdLessThan(meetingId, beforeId, pageable);

        // DESC → ASC 로 뒤집기
        Collections.reverse(list);

        return list.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 엔티티 → DTO 변환
     */
    private ChatMessageDto toDto(MeetingChat chat) {
        Timestamp ts = chat.getSentAt();
        LocalDateTime sentAtLocal = (ts != null)
                ? ts.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDateTime.now();

        Integer userId = (chat.getSender() != null) ? chat.getSender().getId() : null;

        return ChatMessageDto.builder()
                .messageId(chat.getId())
                .meetingId(chat.getMeeting().getId())
                .userId(userId)
                .displayName(chat.getSenderName()) // SYSTEM이면 null 가능
                .message(chat.getMessage())
                .sentAt(sentAtLocal)
                .type(chat.getType() != null ? chat.getType().name() : "CHAT")
                .build();
    }


}
