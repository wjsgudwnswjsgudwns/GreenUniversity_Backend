package com.green.university.dto.response;

import lombok.Data;

@Data
public class MeetingPingResDto {

    /**
     * 이 브라우저 세션이 아직 유효한지 여부.
     */
    private boolean active;

    /**
     * 비유효한 경우 사유:
     * - MEETING_FINISHED
     * - MEETING_CANCELED
     * - SESSION_REPLACED
     * - NOT_JOINED
     * 등.
     */
    private String reason;

    public static MeetingPingResDto inactive(String reason) {
        MeetingPingResDto dto = new MeetingPingResDto();
        dto.setActive(false);
        dto.setReason(reason);
        return dto;
    }
}
