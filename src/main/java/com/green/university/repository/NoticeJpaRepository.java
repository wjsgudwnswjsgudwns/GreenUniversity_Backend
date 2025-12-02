package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Notice;

import java.util.List;

/**
 * JPA repository for {@link Notice} entities.
 */
public interface NoticeJpaRepository extends JpaRepository<Notice, Integer> {

    /**
     * 제목에 특정 키워드가 포함된 공지를 조회합니다 (대소문자 구분 없음).
     *
     * @param keyword 검색어
     * @return 키워드를 포함하는 제목을 가진 공지 목록
     */
    List<Notice> findByTitleContainingIgnoreCase(String keyword);

    /**
     * 본문에 특정 키워드가 포함된 공지를 조회합니다 (대소문자 구분 없음).
     *
     * @param keyword 검색어
     * @return 키워드를 포함하는 본문을 가진 공지 목록
     */
    List<Notice> findByContentContainingIgnoreCase(String keyword);
}