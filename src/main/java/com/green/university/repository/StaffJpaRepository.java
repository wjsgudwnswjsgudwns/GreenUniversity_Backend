package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Staff;

/**
 * JPA repository for {@link Staff} entities.
 */
public interface StaffJpaRepository extends JpaRepository<Staff, Integer> {
}