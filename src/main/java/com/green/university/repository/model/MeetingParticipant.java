package com.green.university.repository.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import lombok.Data;

/**
 * 회의 참가자 정보를 나타내는 엔티티.
 * 교수/학생/직원(기존 user_tb)만 참가자로 사용하며,
 * 예약/취소/알림 메일 발송을 위해 이메일을 함께 저장한다.
 */
@Data
@Entity
@Table(name = "meeting_participant_tb")
public class MeetingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 참가자가 속한 회의.
     */
    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    /**
     * 참가자 계정 (반드시 기존 user_tb 참조).
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "display_name")
    private String displayName;
    /**
     * 참가자 이메일.
     * - 일반적으로 user에 매핑된 이메일을 복제해서 저장.
     * - 예약 취소/알림 메일 발송에 사용.
     */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /**
     * 역할: 'HOST' / 'GUEST'
     * - HOST  : 주최자 (Meeting.host와 동일)
     * - GUEST : 학생, 다른 교수 등
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role;

    /**
     * 참가 상태: 'INVITED', 'JOINED', 'LEFT' 등.
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 이 사용자의 "현재 유효 브라우저/탭 세션"을 식별하는 키.
     * 같은 유저가 새 브라우저에서 다시 접속하면 이 값이 덮어써진다.
     */
    @Column(name = "session_key", length = 100)
    private String sessionKey;

    /**
     * 마지막으로 ping(heartbeat)을 보낸 시각.
     * 접속 중인지 판단하고, 유령 세션을 정리할 때 사용한다.
     */
    @Column(name = "last_active_at")
    private Timestamp lastActiveAt;

    /**
     * 실제 회의에 입장한 시각.
     */
    @Column(name = "joined_at")
    private Timestamp joinedAt;

    /**
     * 회의에서 퇴장한 시각.
     */
    @Column(name = "left_at")
    private Timestamp leftAt;
}
