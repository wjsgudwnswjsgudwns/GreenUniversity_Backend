package com.green.university.repository;

import com.green.university.repository.model.College;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Department;

import java.util.List;

/**
 * JPA repository for {@link Department} entities.
 */

// 학과
public interface DepartmentJpaRepository extends JpaRepository<Department, Integer> {

    // 저장
    // JpaRepository의 save() 메서드 사용

    // 특정 학과 조회
    // JpaRepository의 findById() 메서드 사용

    // 모든 학과 조회
    // JpaRepository의 findAll() 메서드 사용

    // 삭제
    // JpaRepository의 deleteById() 메서드 사용

    // 수정
    // Service에서 findById()로 조회 후 entity의 setter로 값 변경하고 save() 사용

    List<Department> findAllByOrderByIdAsc();
}