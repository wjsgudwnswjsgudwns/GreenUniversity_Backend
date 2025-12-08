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

    // 특정 단과 대학 등록금 정보
    @Query("SELECT c.amount FROM CollTuit c WHERE c.collegeId = :collegeId")
    Integer findAmountByCollegeId(@Param("collegeId") Integer collegeId);

    List<CollTuit> findAllByOrderByCollegeIdAsc();
}