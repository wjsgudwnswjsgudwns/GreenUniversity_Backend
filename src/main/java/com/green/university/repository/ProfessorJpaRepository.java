package com.green.university.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Professor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for {@link Professor} entities.
 */
public interface ProfessorJpaRepository extends JpaRepository<Professor, Integer> {

    List<Professor> findByDepartment_Id(Integer deptId);

    long countByDepartment_Id(Integer deptId);

    Optional<Professor> findByIdAndNameAndEmail(Integer id, String name, String email);
    Optional<Professor> findByNameAndEmail(String name, String email);

    // 학과별 교수 조회 (페이징)
    Page<Professor> findByDeptId(Integer deptId, Pageable pageable);

    // 학과별 교수 수
    long countByDeptId(Integer deptId);

    // 사번으로 조회 (페이징) - 단일 결과를 Page로 반환
    @Query("SELECT p FROM Professor p WHERE p.id = :professorId")
    Page<Professor> findByProfessorId(@Param("professorId") Integer professorId, Pageable pageable);
}