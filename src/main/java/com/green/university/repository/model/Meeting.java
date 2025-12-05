package com.green.university.repository.model;

import java.sql.Timestamp;

import com.green.university.enums.MeetingStatus;
import jakarta.persistence.*;

import lombok.Data;

/**
 * 화상 회의(예약/즉시 회의)를 나타내는 엔티티.
 * - 예약 회의와 즉시 회의를 모두 포함
 */
@Data
@Entity
@Table(name = "meeting_tb")
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /**
     * 회의를 생성한 주최자 (기존 user_tb 참조).
     */
    @ManyToOne
    @JoinColumn(name = "host_user_id", nullable = false)
    private User host;

    /**
     * 회의 유형
     * - 'SCHEDULED' : 예약 회의
     * - 'INSTANT'   : 즉시 회의
     */
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    /**
     * 회의 제목.
     */
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    /**
     * 회의 설명/목적 (옵션).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 회의 시작 예정 시각.
     */
    @Column(name = "start_at", nullable = false)
    private Timestamp startAt;

    /**
     * 회의 종료 예정 시각.
     */
    @Column(name = "end_at", nullable = false)
    private Timestamp endAt;

    /**
     * 회의 상태
     * - 'SCHEDULED'   : 예약됨
     * - 'IN_PROGRESS' : 진행 중
     * - 'FINISHED'    : 종료
     * - 'CANCELED'    : 취소
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MeetingStatus status;

    /**
     * Janus videoroom에서 사용할 방 번호.
     */
    @Column(name = "room_number", nullable = false)
    private Integer roomNumber;

    /**
     * 레코드 생성 시각.
     * (DB 트리거나 애플리케이션 레벨에서 설정 가능)
     */
    @Column(name = "created_at")
    private Timestamp createdAt;

    /**
     * 레코드 마지막 수정 시각.
     */
    @Column(name = "updated_at")
    private Timestamp updatedAt;

    /**
     * 회의방에 아무도 남지 않은 마지막 시각
     * - 마지막 참가자가 나갈 때 찍고
     * - 누군가 다시 들어오면 null로 초기화
     * - 이 값 + 30분 넘으면 자동 FINISHED 처리
     */
    @Column(name = "last_empty_at")
    private Timestamp lastEmptyAt;
}
