package com.green.university.controller;

import com.green.university.dto.ChatMessageDto;
import com.green.university.dto.MediaStateSignalMessageDto;
import com.green.university.service.MeetingChatService;
import com.green.university.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MeetingWebSocketController {

    private final MeetingChatService meetingChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private  final MeetingService meetingService;
    @MessageMapping("/meetings/{meetingId}/chat")
    public void handleChat(
            @DestinationVariable Integer meetingId,
            @Payload ChatMessageDto payload
    ) {
        // 0) payload null 체크
        if (payload == null) {
            log.warn("[MeetingWebSocketController] payload 가 null 입니다.");
            return;
        }

        log.debug("[MeetingWebSocketController] 수신 payload={}", payload);

        // 1) 메시지 내용 검증
        String messageText = payload.getMessage();
        if (messageText == null || messageText.trim().isEmpty()) {
            log.debug("[MeetingWebSocketController] 빈 메시지 전송 시도. 무시. payload={}", payload);
            return;
        }

        // 2) 사용자 정보 (전부 payload 기준)
        Integer userId = payload.getUserId();
        String displayName = payload.getDisplayName();

        if (userId == null) {
            log.warn("[MeetingWebSocketController] userId 가 없습니다. payload={}", payload);
            return;
        }
        if (displayName == null || displayName.isBlank()) {
            displayName = "참가자";
        }

        // 3) 서비스 호출 (DB 저장)
        ChatMessageDto saved = meetingChatService.saveChatMessage(
                meetingId,
                userId,
                displayName,
                messageText,
                payload.getType()
        );

        // 4) 구독자에게 브로드캐스트
        String destination = String.format("/sub/meetings/%d/chat", meetingId);
        messagingTemplate.convertAndSend(destination, saved);

        log.debug("[MeetingWebSocketController] 채팅 전송 완료. dest={}, saved={}", destination, saved);
    }

    @MessageMapping("/meetings/{meetingId}/signals")
    public void handleMediaSignal(
            @DestinationVariable Integer meetingId,
            @Payload MediaStateSignalMessageDto payload
    ) {
        if (payload == null) {
            log.warn("[MeetingWebSocketController] MEDIA_STATE payload 가 null 입니다.");
            return;
        }

        // meetingId는 path 변수 기준으로 강제 세팅
        payload.setMeetingId(meetingId);

        // type 기본값
        if (payload.getType() == null || payload.getType().isBlank()) {
            payload.setType("MEDIA_STATE");
        }

        if (payload.getDisplay() == null || payload.getDisplay().isBlank()) {
            log.warn("[MeetingWebSocketController] MEDIA_STATE display 없음. payload={}", payload);
            return;
        }

        log.debug("[MeetingWebSocketController] MEDIA_STATE 수신: {}", payload);

        // 그냥 그대로 브로드캐스트 (DB 저장 없음, 단순 신호)
        String destination = String.format("/sub/meetings/%d/signals", meetingId);
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("[MeetingWebSocketController] MEDIA_STATE 브로드캐스트 dest={}, payload={}",
                destination, payload);
    }


}
