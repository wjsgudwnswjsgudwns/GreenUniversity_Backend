package com.green.university.repository;

import com.green.university.repository.model.College;
import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.Room;

import java.util.List;

/**
 * JPA repository for {@link Room} entities.
 */
public interface RoomJpaRepository extends JpaRepository<Room, String> {

    List<Room> findAllByOrderByIdAsc();
}