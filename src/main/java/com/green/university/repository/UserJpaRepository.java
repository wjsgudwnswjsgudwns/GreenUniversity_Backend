package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.User;

/**
 * JPA repository for {@link User} entities.
 */
public interface UserJpaRepository extends JpaRepository<User, Integer> {
}