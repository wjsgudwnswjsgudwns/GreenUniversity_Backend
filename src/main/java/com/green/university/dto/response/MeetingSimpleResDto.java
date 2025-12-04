package com.green.university.dto.response;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * 즉시 회의 / 예약 생성 후 공통으로 사용할 응답 DTO.
 */
@Data
public class MeetingSimpleResDto {

    private Integer meetingId;
    private String type;        // SCHEDULED / INSTANT
    private String title;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Integer roomNumber;
    private String status;
}