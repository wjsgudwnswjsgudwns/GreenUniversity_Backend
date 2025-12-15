package com.green.university.repository.model;

import java.sql.Timestamp;

import com.green.university.enums.ChatMessageType;
import jakarta.persistence.*;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 회의방 텍스트 채팅 메시지 엔티티.
 * 교수/학생/직원 계정만 발신자로 사용.
 */
@Data
@Entity
@Table(name = "meeting_chat_tb")
public class MeetingChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 채팅이 속한 회의.
     */
    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    /**
     * 메시지 보낸 사용자 (반드시 user_tb 참조).
     */
    @ManyToOne(optional = true)
    @JoinColumn(name = "sender_user_id", nullable = true)
    private User sender;

    /**
     * 당시 표기용 이름/닉네임.
     * (나중에 이름이 바뀌어도 로그에는 이 값이 남도록 별도로 저장)
     */
    @Column(name = "sender_name", nullable = true, length = 50)
    private String senderName;

    /**
     * 채팅 메시지 본문.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType type;

    /**
     * 전송 시각.
     */
    @CreationTimestamp
    @Column(name = "sent_at", nullable = false)
    private Timestamp sentAt;

    /**
     * 보관 만료 시각 (예: sent_at + 30일).
     * 만료 정책에 따라 주기적으로 삭제 가능.
     */
    @Column(name = "expired_at")
    private Timestamp expiredAt;
}
