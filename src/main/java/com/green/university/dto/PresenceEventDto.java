// com.green.university.dto.PresenceEventDto
package com.green.university.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceEventDto {

    /**
     * JOIN | LEAVE | SYNC
     */
    private String type;

    private Integer meetingId;

    // 단일 이벤트용
    private Integer userId;
    private String displayName;
    private String role;
    private Boolean joined;

    // SYNC용
    private List<ParticipantDto> participants;

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantDto {
        private Integer userId;
        private String displayName;
        private String role;
        private Boolean joined;
    }
}
