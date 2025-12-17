package com.green.university.controller;

import com.green.university.dto.ChatMessageDto;
import com.green.university.service.MeetingChatService;
import com.green.university.service.MeetingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MeetingRestController {

    private final MeetingChatService meetingChatService;
    private final MeetingService meetingService;
    @GetMapping("/api/meetings/{meetingId}/chat/messages")
    public List<ChatMessageDto> getMessages(
            @PathVariable Integer meetingId,
            @RequestParam(required = false) Integer afterId,
            @RequestParam(required = false) Integer beforeId,
            @RequestParam(required = false) Integer size
    ) {
        if (afterId != null) {
            return meetingChatService.getMessagesAfter(meetingId, afterId, size);
        } else if (beforeId != null) {
            return meetingChatService.getMessagesBefore(meetingId, beforeId, size);
        } else {
            return meetingChatService.getRecentMessages(meetingId, size);
        }
    }
    @Data
    public static class LeaveKeepaliveReq {
        private String sessionKey;
    }
    /**
     * 탭 닫기/뒤로가기/새로고침 등에서 best-effort로 보내는 leave.
     * Authorization 없이 sessionKey로만 검증하는 것을 권장(성공률↑).
     */
    @PostMapping("/api/meetings/{meetingId}/participants/leave-keepalive")
    public ResponseEntity<Void> leaveKeepalive(
            @PathVariable Integer meetingId,
            @RequestBody(required = false) LeaveKeepaliveReq req
    ) {
        if (req == null || req.getSessionKey() == null || req.getSessionKey().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        meetingService.leaveKeepalive(meetingId, req.getSessionKey());
        return ResponseEntity.ok().build();
    }
}
