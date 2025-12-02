package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.College;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JPA repository for {@link College} entities.
 */
public interface CollegeJpaRepository extends JpaRepository<College, Integer> {

    // 단과대학 저장
    // JpaRepository의 save() 메서드 사용

    // 모든 단과 대학 조회
    // JpaRepository의 findAll() 메서드 사용

    // 해당 이름을 가진 단과대가 몇 개 있는지 개수를 반환
    public int countByName(String name);

    // 특정 단과대학 조회
    // JpaRepository의 findById() 메서드 사용

    // 아이디로 삭제
    // JpaRepository의 deleteById() 메서드 사용

}
