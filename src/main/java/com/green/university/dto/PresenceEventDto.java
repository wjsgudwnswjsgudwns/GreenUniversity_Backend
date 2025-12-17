// com.green.university.dto.PresenceEventDto
package com.green.university.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PresenceEventDto {

    private String type; // JOIN | LEAVE | FORCE_LEAVE | SYNC
    private Integer meetingId;
    private Integer userId;
    private Boolean joined;
    private String displayName;
    private String role; // HOST | PARTICIPANT

    private List<ParticipantDto> participants; // SYNC용

    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ParticipantDto {
        private Integer userId;
        private String displayName;
        private String role;
        private boolean joined;
    }
}
