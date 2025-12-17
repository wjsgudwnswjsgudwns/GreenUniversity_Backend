package com.green.university.dto;

import lombok.Data;

@Data
public class MediaStateSignalMessageDto {

    private Integer meetingId;
    private Integer userId;
    private String display;

    private Boolean audio;         // true = 켬, false = 끔
    private Boolean video;         // true = 켬, false = 끔

    private Boolean videoDeviceLost;
    private Long ts;
    private String type = "MEDIA_STATE"; // 나중에 타입 늘릴 때 구분용
}
