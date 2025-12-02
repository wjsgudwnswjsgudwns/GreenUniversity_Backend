package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.NoticeFile;

/**
 * JPA repository for {@link NoticeFile} entities.
 */
public interface NoticeFileJpaRepository extends JpaRepository<NoticeFile, String> {
}