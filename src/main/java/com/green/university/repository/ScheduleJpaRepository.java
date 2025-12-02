package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Schedule;

/**
 * JPA repository for {@link Schedule} entities.
 */
public interface ScheduleJpaRepository extends JpaRepository<Schedule, Integer> {
}