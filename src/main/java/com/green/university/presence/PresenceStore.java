package com.green.university.presence;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceStore {

    @Data
    @Builder
    public static class State {
        private Integer meetingId;
        private Integer userId;
        private String displayName;
        private String role;         // HOST / PARTICIPANT
        private String sessionKey;   // 현재 유효 세션
        private long lastSeenAt;     // epoch millis
        private String stompSessionId;
    }

    // meetingId -> (userId -> state)
    private final Map<Integer, Map<Integer, State>> byMeeting = new ConcurrentHashMap<>();
    // stompSessionId -> (meetingId, userId)
    private final Map<String, int[]> byStomp = new ConcurrentHashMap<>();

    public String joinOrReplace(Integer meetingId, Integer userId, String displayName, String role) {
        String newSessionKey = UUID.randomUUID().toString();
        long now = Instant.now().toEpochMilli();

        Map<Integer, State> meetingMap =
                byMeeting.computeIfAbsent(meetingId, k -> new ConcurrentHashMap<>());

        // ✅ 기존 state가 있으면, 거기 묶여 있던 byStomp를 먼저 제거
        State prev = meetingMap.get(userId);
        if (prev != null && prev.getStompSessionId() != null) {
            byStomp.remove(prev.getStompSessionId());
        }

        meetingMap.put(userId, State.builder()
                .meetingId(meetingId)
                .userId(userId)
                .displayName(displayName)
                .role(role != null ? role : "PARTICIPANT")
                .sessionKey(newSessionKey)
                .lastSeenAt(now)
                .stompSessionId(null) // 새로 바인딩될 예정
                .build());

        return newSessionKey;
    }

    public State get(Integer meetingId, Integer userId) {
        Map<Integer, State> m = byMeeting.get(meetingId);
        if (m == null) return null;
        return m.get(userId);
    }

    /**
     * @return true면 유효(active), false면 비유효(inactive: NOT_JOINED or SESSION_REPLACED)
     */
    public boolean heartbeat(Integer meetingId, Integer userId, String clientSessionKey) {
        State s = get(meetingId, userId);
        if (s == null) return false;

        String serverKey = s.getSessionKey();
        if (serverKey != null && clientSessionKey != null && !serverKey.equals(clientSessionKey)) {
            return false; // SESSION_REPLACED
        }

        s.setLastSeenAt(Instant.now().toEpochMilli());
        return true;
    }

    public void leave(Integer meetingId, Integer userId) {
        Map<Integer, State> m = byMeeting.get(meetingId);
        if (m == null) return;

        State removed = m.remove(userId);
        if (removed != null && removed.getStompSessionId() != null) {
            byStomp.remove(removed.getStompSessionId());
        }

        if (m.isEmpty()) byMeeting.remove(meetingId);
    }

    public List<State> list(Integer meetingId) {
        Map<Integer, State> m = byMeeting.get(meetingId);
        if (m == null) return List.of();
        return new ArrayList<>(m.values());
    }

    public void bindStompSession(Integer meetingId, Integer userId, String stompSessionId) {
        if (stompSessionId == null) return;
        State s = get(meetingId, userId);
        if (s == null) return;

        s.setStompSessionId(stompSessionId);
        byStomp.put(stompSessionId, new int[]{meetingId, userId});
    }

    public int[] findByStompSession(String stompSessionId) {
        if (stompSessionId == null) return null;
        return byStomp.get(stompSessionId);
    }

    /** TTL 스위퍼가 쓸 수 있게 전체 meetingId 목록 */
    public Set<Integer> meetingIds() {
        return new HashSet<>(byMeeting.keySet());
    }
    public Integer findUserIdBySessionKey(Integer meetingId, String sessionKey) {
        Map<Integer, State> m = byMeeting.get(meetingId);
        if (m == null) return null;

        for (Map.Entry<Integer, State> e : m.entrySet()) {
            if (sessionKey.equals(e.getValue().getSessionKey())) {
                return e.getKey();
            }
        }
        return null;
    }

}
