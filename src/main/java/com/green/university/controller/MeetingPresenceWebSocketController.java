// com.green.university.controller.MeetingPresenceWebSocketController
package com.green.university.controller;

import com.green.university.dto.PresenceEventDto;
import com.green.university.presence.PresenceStore;
import com.green.university.repository.MeetingParticipantJpaRepository;
import com.green.university.repository.model.MeetingParticipant;
import com.green.university.service.MeetingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MeetingPresenceWebSocketController {

    private final MeetingService meetingService;
    private final PresenceStore presenceStore;

    @Data
    public static class SyncReq {
        private Integer meetingId;
        private Integer userId;
        private String sessionKey;
    }
    /**
     * 클라이언트가 SYNC 요청할 때
     * /pub/meetings/{meetingId}/presence/sync
     */
    @MessageMapping("/meetings/{meetingId}/presence/sync")
    public void syncPresence(
            @DestinationVariable Integer meetingId,
            SyncReq req,
            @Header("simpSessionId") String stompSessionId
    ) {
        // 1) (선택) userId가 왔으면 stompSession 바인딩
        if (req != null && req.getUserId() != null) {
            // sessionKey 검증을 엄격히 하고 싶으면:
            // presenceStore.heartbeat(meetingId, req.getUserId(), req.getSessionKey());
            presenceStore.bindStompSession(meetingId, req.getUserId(), stompSessionId);
        }

        // 2) SYNC 전송
        meetingService.broadcastPresenceSync(meetingId);
        log.debug("[Presence] SYNC requested meetingId={}, stompSessionId={}", meetingId, stompSessionId);
    }
}
