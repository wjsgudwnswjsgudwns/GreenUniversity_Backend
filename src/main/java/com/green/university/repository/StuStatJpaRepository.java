package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.StuStat;

/**
 * JPA repository for {@link StuStat} entities.
 */
public interface StuStatJpaRepository extends JpaRepository<StuStat, Integer> {
}