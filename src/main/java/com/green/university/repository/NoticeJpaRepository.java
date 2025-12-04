package com.green.university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.green.university.repository.model.Notice;

import java.util.List;

/**
 * JPA repository for {@link Notice} entities.
 */
public interface NoticeJpaRepository extends JpaRepository<Notice, Integer> {

    /**
     * 전체 공지를 페이징 처리하여 조회 (ID 내림차순 - 최신순)
     */
    @Query("SELECT n FROM Notice n ORDER BY n.id DESC")
    Page<Notice> findAllByOrderByIdDesc(Pageable pageable);

    /**
     * 제목에 특정 키워드가 포함된 공지를 페이징 처리하여 조회 (ID 내림차순)
     */
    @Query("SELECT n FROM Notice n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY n.id DESC")
    Page<Notice> findByTitleContainingIgnoreCaseOrderByIdDesc(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 본문에 특정 키워드가 포함된 공지를 페이징 처리하여 조회 (ID 내림차순)
     */
    @Query("SELECT n FROM Notice n WHERE LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY n.id DESC")
    Page<Notice> findByContentContainingIgnoreCaseOrderByIdDesc(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목 또는 본문에 키워드가 포함된 공지를 페이징 처리하여 조회 (ID 내림차순)
     */
    @Query("SELECT n FROM Notice n WHERE LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY n.id DESC")
    Page<Notice> findByTitleOrContentContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 제목에 특정 키워드가 포함된 공지를 조회 (페이징 없음)
     */
    List<Notice> findByTitleContainingIgnoreCase(String keyword);

    /**
     * 본문에 특정 키워드가 포함된 공지를 조회 (페이징 없음)
     */
    List<Notice> findByContentContainingIgnoreCase(String keyword);
}