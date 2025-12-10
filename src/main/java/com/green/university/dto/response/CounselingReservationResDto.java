package com.green.university.dto.response;

import com.green.university.enums.CounselingReservationStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CounselingReservationResDto {
    private Long reservationId;
    private Long slotId;
    private Integer studentId;
    private String studentName;
    private CounselingReservationStatus status;
    private String studentMemo;
    private Integer meetingId;
    private LocalDateTime createdAt;
    private LocalDateTime canceledAt;
    private LocalDateTime slotStartAt;
    private LocalDateTime slotEndAt;
    private String professorName; // 예약한 교수 이름
}
