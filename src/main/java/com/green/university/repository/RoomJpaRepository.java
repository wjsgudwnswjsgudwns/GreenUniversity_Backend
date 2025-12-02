package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Room;

/**
 * JPA repository for {@link Room} entities.
 */
public interface RoomJpaRepository extends JpaRepository<Room, String> {
}