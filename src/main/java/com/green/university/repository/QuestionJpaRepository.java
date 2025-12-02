package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Question;

/**
 * JPA repository interface for {@link Question} entities.
 *
 * Provides basic CRUD operations and allows future extension for
 * custom query methods if needed.
 */
public interface QuestionJpaRepository extends JpaRepository<Question, Integer> {
    // 추가적인 쿼리 메서드가 필요하다면 여기에 정의합니다.
}