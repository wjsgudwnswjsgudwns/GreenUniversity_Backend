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

    List<Department> findAllByOrderByIdAsc();
}