package com.green.university.controller;

import com.green.university.dto.ChatMessageDto;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class MeetingWebSocketController {
    @MessageMapping("/meetings/{meetingId}/chat")
    @SendTo("/sub/meetings/{meetingId}/chat")
    public ChatMessageDto chat(@DestinationVariable Integer meetingId, ChatMessageDto message) {
        // 여기서 message 에 meetingId를 강제로 세팅 (신뢰용)
        message.setMeetingId(meetingId);

        // TODO: 나중에 Principal에서 userId 꺼내서 message.setUserId(진짜 아이디)
        // 나중에 DB 저장, 필터링 등도 여기서 처리 가능

        message.setSentAt(LocalDateTime.now());

        return message; // 반환된 객체가 /sub/... 구독자에게 브로드캐스트
    }
}
