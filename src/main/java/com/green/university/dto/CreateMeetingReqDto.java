package com.green.university.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateMeetingReqDto {

        private String title;
        private String description;

        private LocalDateTime startAt;
        private LocalDateTime endAt;

        // 나중에 학생/교수 여러 명을 초대할 때 userId 리스트를 받을 수도 있음
        // private List<Integer> participantUserIds;
}
