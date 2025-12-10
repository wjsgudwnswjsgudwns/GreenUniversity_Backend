package com.green.university.repository;

import com.green.university.enums.CounselingReservationStatus;
import com.green.university.repository.model.CounselingReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;

public interface CounselingReservationJpaRepository extends JpaRepository<CounselingReservation, Long> {
    // 학생 기준, 기간 내 예약 리스트 (CANCELED 제외)
    List<CounselingReservation> findByStudent_IdAndStatusNotAndSlot_StartAtBetweenOrderBySlot_StartAt(
            Integer studentId,
            CounselingReservationStatus status,
            Timestamp from,
            Timestamp to
    );

    // 교수 기준, 기간 내 예약 리스트 (CANCELED 제외)
    List<CounselingReservation> findBySlot_Professor_IdAndStatusNotAndSlot_StartAtBetweenOrderBySlot_StartAt(
            Integer professorId,
            CounselingReservationStatus status,
            Timestamp from,
            Timestamp to
    );

    // 학생 기준, 시간 겹치는 예약 여부
    boolean existsByStudent_IdAndStatusAndSlot_StartAtLessThanAndSlot_EndAtGreaterThan(
            Integer studentId,
            CounselingReservationStatus status,
            Timestamp slotEnd,
            Timestamp slotStart
    );
    boolean existsBySlot_IdAndStatus(Long slotId, CounselingReservationStatus status);
    // 이 slot에 예약이 하나라도 있는지 (삭제 제한용)
    boolean existsBySlot_Id(Long slotId);

    // 특정 슬롯의 예약 전체 (교수 상세 보기 / 슬롯 상태 계산용)
    List<CounselingReservation> findBySlot_Id(Long slotId);
}
