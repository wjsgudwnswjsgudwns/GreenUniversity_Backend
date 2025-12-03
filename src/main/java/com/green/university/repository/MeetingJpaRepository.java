package com.green.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Meeting;

public interface MeetingJpaRepository extends JpaRepository<Meeting, Integer> {

    // 주최자 기준 회의 목록
    List<Meeting> findByHost_IdOrderByStartAtDesc(Integer hostUserId);

    // 상태 + 주최자 기준으로 필터링하고 싶을 때 사용 가능
    List<Meeting> findByHost_IdAndStatusOrderByStartAtDesc(Integer hostUserId, String status);
}
