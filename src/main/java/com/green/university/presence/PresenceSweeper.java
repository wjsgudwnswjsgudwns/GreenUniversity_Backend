package com.green.university.presence;

import com.green.university.dto.PresenceEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class PresenceSweeper {

    private final PresenceStore presenceStore;
    private final SimpMessagingTemplate messagingTemplate;

    // 유령 기준: 60초 (원하면 30초로 줄여도 됨)
    private static final long STALE_MS = 60_000L;

    @Scheduled(fixedDelay = 10_000L)
    public void sweep() {
        long now = Instant.now().toEpochMilli();

        for (Integer meetingId : presenceStore.meetingIds()) {
            List<PresenceStore.State> list = presenceStore.list(meetingId);
            for (PresenceStore.State s : list) {
                if (s == null) continue;

                long age = now - s.getLastSeenAt();
                if (age <= STALE_MS) continue;

                Integer userId = s.getUserId();
                presenceStore.leave(meetingId, userId);

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

                log.debug("[PresenceSweeper] stale leave meetingId={}, userId={}, ageMs={}", meetingId, userId, age);
            }
        }
    }
}
