package com.green.university.repository.model;

import com.green.university.enums.CounselingReservationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.sql.Timestamp;

@Entity
@Data
@Table(name = "counseling_reservation_tb")
public class CounselingReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 슬롯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private CounselingSlot slot;

    // 학생
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CounselingReservationStatus status;

    @Column(name = "student_memo", length = 1000)
    private String studentMemo;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "canceled_at")
    private Timestamp canceledAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @Column(name = "meeting_id")
    private Integer meetingId;
}