package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Scholarship;

/**
 * JPA repository for {@link Scholarship} entities.
 */
public interface ScholarshipJpaRepository extends JpaRepository<Scholarship, Integer> {
}