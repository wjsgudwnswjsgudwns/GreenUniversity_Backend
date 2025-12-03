package com.green.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.MeetingParticipant;

public interface MeetingParticipantJpaRepository extends JpaRepository<MeetingParticipant, Integer> {

    // 특정 회의의 모든 참가자
    List<MeetingParticipant> findByMeeting_Id(Integer meetingId);

    // 특정 유저가 참가자인 회의 목록
    List<MeetingParticipant> findByUser_Id(Integer userId);
}
