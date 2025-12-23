package com.green.university.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.NoticeFile;

/**
 * JPA repository for {@link NoticeFile} entities.
 */
public interface NoticeFileJpaRepository extends JpaRepository<NoticeFile, String> {
    /**
     * 공지사항 ID로 첨부파일 목록 조회
     */
    List<NoticeFile> findByNoticeId(Integer noticeId);
}