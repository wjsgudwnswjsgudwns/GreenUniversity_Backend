package com.green.university.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.green.university.repository.model.SyllaBus;

/**
 * JPA repository for {@link SyllaBus} entities.
 */
public interface SyllaBusJpaRepository extends JpaRepository<SyllaBus, Integer> {

    void deleteBySubjectId(Integer subjectId);

}