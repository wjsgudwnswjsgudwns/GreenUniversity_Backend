package com.green.university.dto.response;
import java.time.LocalDateTime;

import lombok.Data;
/**
 * /join-info 에서 React + Janus가 사용할 데이터.
 */
@Data
public class MeetingJoinInfoResDto {

    private Integer meetingId;
    private String title;
    private Integer roomNumber;

    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;          // SCHEDULED / IN_PROGRESS / ...

    private Integer userId;         // 실제 식별용
    private String displayName;     // Janus에 넘길 닉네임 (예: "홍길동(교수)")
    private String userRole;        // student / professor / staff

    private String sessionKey;

    private Boolean isHost;      // 이 사용자가 HOST 인지 여부
    private Integer hostUserId;  // 회의 주최자 userId

}