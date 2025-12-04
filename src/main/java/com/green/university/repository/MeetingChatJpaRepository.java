package com.green.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.MeetingChat;

public interface MeetingChatJpaRepository extends JpaRepository<MeetingChat, Integer> {

    // 특정 회의의 채팅 메시지를 시간 순으로 조회
    List<MeetingChat> findByMeeting_IdOrderBySentAtAsc(Integer meetingId);

    List<MeetingChat> findTop100ByMeeting_IdOrderBySentAtDesc(Integer meetingId);
}
