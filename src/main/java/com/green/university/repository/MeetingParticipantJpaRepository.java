package com.green.university.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.MeetingParticipant;

public interface MeetingParticipantJpaRepository extends JpaRepository<MeetingParticipant, Integer> {

    // 특정 회의의 모든 참가자
    List<MeetingParticipant> findByMeeting_Id(Integer meetingId);

    // 특정 유저가 참가자인 회의 목록
    List<MeetingParticipant> findByUser_Id(Integer userId);

    // 특정 회의 + 특정 유저 1명
    Optional<MeetingParticipant> findByMeeting_IdAndUser_Id(Integer meetingId, Integer userId);

    // 내가 참여자로 들어간 방들을, meeting.startAt 기준 내림차순으로
    List<MeetingParticipant> findByUser_IdOrderByMeeting_StartAtDesc(Integer userId);

    // 해당 회의에 아직 JOINED 상태인 사람이 있는지 여부
    boolean existsByMeeting_IdAndStatus(Integer meetingId, String status);

    List<MeetingParticipant> findByMeeting_IdAndStatus(Integer meetingId, String status);
}
