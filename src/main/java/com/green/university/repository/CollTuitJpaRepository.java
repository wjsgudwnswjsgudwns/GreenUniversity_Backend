package com.green.university.repository;

import com.green.university.repository.model.College;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.CollTuit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * JPA repository for {@link CollTuit} entities.
 */
// 단과대별 등록금 정보
public interface CollTuitJpaRepository extends JpaRepository<CollTuit, Integer> {
    //    @Query("SELECT c.amount FROM CollTuit c WHERE c.collegeId = :collegeId")
    //    Integer findAmountByCollegeId(@Param("collegeId") Integer collegeId);

    // 특정 단과 대학 등록금 정보
    public Integer findAmountByCollegeId(Integer collegeId);

    // 저장
    // JpaRepository의 save() 메서드 사용

    // 전체 조회
    // JpaRepository의 findAll() 메서드 사용

    // 삭제
    // JpaRepository의 deleteById() 메서드 사용

    // 수정
    // Service에서 findById()로 조회 후 entity.setAmount() 하고 save() 사용

    List<CollTuit> findAllByOrderByCollegeIdAsc();
}