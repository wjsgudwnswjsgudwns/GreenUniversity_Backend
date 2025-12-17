package com.green.university.presence;

import com.green.university.dto.PresenceEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class PresenceDisconnectListener {

    private final PresenceStore presenceStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final MediaStateStore mediaStateStore;

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor sha = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = sha.getSessionId();

        int[] pair = presenceStore.findByStompSession(sessionId);
        if (pair == null) return;

        Integer meetingId = pair[0];
        Integer userId = pair[1];

        PresenceStore.State cur = presenceStore.get(meetingId, userId);
        if (cur == null) return;

        if (!sessionId.equals(cur.getStompSessionId())) {
            log.debug("[PresenceDisconnect] ignore stale disconnect. meetingId={}, userId={}, stompSessionId={}, currentStomp={}",
                    meetingId, userId, sessionId, cur.getStompSessionId());
            return;
        }

        presenceStore.leave(meetingId, userId);
        mediaStateStore.remove(meetingId, userId); // 미디어 상태 삭제

        PresenceEventDto payload = PresenceEventDto.builder()
                .type("LEAVE")
                .meetingId(meetingId)
                .userId(userId)
                .build();

        messagingTemplate.convertAndSend(
                String.format("/sub/meetings/%d/presence", meetingId),
                payload
        );

        log.debug("[PresenceDisconnect] meetingId={}, userId={}, stompSessionId={}", meetingId, userId, sessionId);
    }
}
