package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Professor;

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


}