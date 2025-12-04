package com.green.university.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    private Integer meetingId;
    private Integer userId;
    private String displayName;
    private String type; // "CHAT", "JOIN", "LEAVE" 등
    private String message;
    private LocalDateTime sentAt;

}