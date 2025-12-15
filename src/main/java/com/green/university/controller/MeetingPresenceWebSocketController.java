// com.green.university.controller.MeetingPresenceWebSocketController
package com.green.university.controller;

import com.green.university.dto.PresenceEventDto;
import com.green.university.repository.MeetingParticipantJpaRepository;
import com.green.university.repository.model.MeetingParticipant;
import com.green.university.service.MeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class MeetingPresenceWebSocketController {

    private  final MeetingService meetingService;

    /**
     * 클라이언트가 SYNC 요청할 때
     * /pub/meetings/{meetingId}/presence/sync
     */
    @MessageMapping("/meetings/{meetingId}/presence/sync")
    public void syncPresence(@DestinationVariable Integer meetingId) {
        meetingService.broadcastPresenceSync(meetingId);
        log.debug("[Presence] SYNC requested meetingId={}", meetingId);
    }
}
